package org.jboss.as.console.client.standalone.runtime;

import java.util.ArrayList;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TreeItem;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.Console;
import org.jboss.as.console.client.core.NameTokens;
import org.jboss.as.console.client.plugins.RuntimeExtensionMetaData;
import org.jboss.as.console.client.plugins.RuntimeExtensionRegistry;
import org.jboss.as.console.client.plugins.RuntimeGroup;
import org.jboss.as.console.client.shared.model.SubsystemRecord;
import org.jboss.as.console.client.widgets.nav.Predicate;
import org.jboss.as.console.client.widgets.tree.GroupItem;
import org.jboss.ballroom.client.layout.LHSNavTree;
import org.jboss.ballroom.client.layout.LHSNavTreeItem;
import org.jboss.ballroom.client.layout.LHSTreeSection;

/**
 * @author Heiko Braun
 * @date 11/2/11
 */
public class StandaloneRuntimeNavigation {

    private VerticalPanel stack;
    private VerticalPanel layout;

    private List<Predicate> metricPredicates = new ArrayList<Predicate>();
    private List<Predicate> runtimePredicates = new ArrayList<Predicate>();

    private ScrollPanel scroll;
    private LHSNavTree navigation;
    private LHSTreeSection metricLeaf;
    private LHSTreeSection runtimeLeaf;

    public Widget asWidget()
    {
        layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");

        stack = new VerticalPanel();
        stack.setStyleName("fill-layout-width");

        // ----------------------------------------------------


        navigation = new LHSNavTree("standalone-runtime");
        navigation.getElement().setAttribute("aria-label", "Runtime Tasks");

        // ----------------------------------------------------

        TreeItem serverLeaf = new LHSTreeSection("Server", true);

        LHSNavTreeItem server = new LHSNavTreeItem("Overview", NameTokens.StandaloneServerPresenter);

        serverLeaf.addItem(server);
        serverLeaf.addItem(new LHSNavTreeItem("Manage Deployments", NameTokens.DeploymentBrowserPresenter));
        serverLeaf.addItem(new LHSNavTreeItem("Patch Management", NameTokens.PatchingPresenter));
        navigation.addItem(serverLeaf);


        // -------------

        metricLeaf = new LHSTreeSection("Status");
        navigation.addItem(metricLeaf);

        LHSNavTreeItem datasources = new LHSNavTreeItem("Datasources", NameTokens.DataSourceMetricPresenter);
        LHSNavTreeItem jmsQueues = new LHSNavTreeItem("JMS Destinations", NameTokens.JmsMetricPresenter);
        LHSNavTreeItem web = new LHSNavTreeItem("Web", NameTokens.WebMetricPresenter);
        LHSNavTreeItem jpa = new LHSNavTreeItem("JPA", NameTokens.JPAMetricPresenter);
        LHSNavTreeItem ws = new LHSNavTreeItem("Webservices", NameTokens.WebServiceRuntimePresenter);
        LHSNavTreeItem naming = new LHSNavTreeItem("JNDI View", NameTokens.JndiPresenter);

        metricPredicates.add(new Predicate("datasources", datasources));
        metricPredicates.add(new Predicate("messaging", jmsQueues));
        metricPredicates.add(new Predicate("web", web));
        metricPredicates.add(new Predicate("jpa", jpa));
        metricPredicates.add(new Predicate("webservices", ws));
        metricPredicates.add(new Predicate("naming", naming));


        // Extension based additions
        RuntimeExtensionRegistry registry = Console.getRuntimeLHSItemExtensionRegistry();
        List<RuntimeExtensionMetaData> menuExtensions = registry.getExtensions();
        for (RuntimeExtensionMetaData ext : menuExtensions) {

            if(RuntimeGroup.METRICS.equals(ext.getGroup()))
            {
                metricPredicates.add(
                        new Predicate(
                                ext.getKey(), new LHSNavTreeItem(ext.getName(), ext.getToken())
                        )
                );
            }
            else if(RuntimeGroup.OPERATiONS.equals(ext.getGroup()))
            {
                runtimePredicates.add(
                        new Predicate(
                                ext.getKey(), new LHSNavTreeItem(ext.getName(), ext.getToken())
                        )
                );
            }
            else
            {
                Log.warn("Invalid runtime group for extension: "+ext.getGroup());
            }
        }

        // ---

        runtimeLeaf = new LHSTreeSection("Runtime Operations");
        navigation.addItem(runtimeLeaf);

        LHSNavTreeItem osgi = new LHSNavTreeItem("OSGi", NameTokens.OSGiRuntimePresenter);
        runtimePredicates.add(new Predicate("osgi", osgi));

        // ----------------------------------------------------

        navigation.expandTopLevel();

        stack.add(navigation);
        layout.add(stack);

        scroll = new ScrollPanel(layout);

        return scroll;
    }



    public void setSubsystems(List<SubsystemRecord> subsystems) {
        metricLeaf.removeItems();
        runtimeLeaf.removeItems();
        runtimeLeaf.setVisible(true);

        if(subsystems.isEmpty()) return;

        final GroupItem platformGroup = new GroupItem("Platform");
        platformGroup.addItem(new LHSNavTreeItem("JVM", NameTokens.VirtualMachine));
        platformGroup.addItem(new LHSNavTreeItem("Environment", NameTokens.EnvironmentPresenter));

        metricLeaf.addItem(platformGroup);

        final GroupItem subsystemGroup = new GroupItem("Subsystems");
        // match subsystems
        for(SubsystemRecord subsys : subsystems) {
            for(Predicate predicate : metricPredicates) {
                if(predicate.matches(subsys.getKey()))
                    subsystemGroup.addItem(predicate.getNavItem());
            }

            for(Predicate predicate : runtimePredicates) {
                if(predicate.matches(subsys.getKey()))
                    runtimeLeaf.addItem(predicate.getNavItem());
            }
        }

        metricLeaf.addItem(subsystemGroup);
        subsystemGroup.setState(true);
        platformGroup.setState(true);

        // empty runtime operations
        runtimeLeaf.setVisible(runtimeLeaf.getChildCount()>0);
        navigation.expandTopLevel();
    }
}
