package org.jboss.as.console.client.tools;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.event.shared.EventBus;
import com.google.inject.Inject;
import com.gwtplatform.mvp.client.PopupView;
import com.gwtplatform.mvp.client.PresenterWidget;
import com.gwtplatform.mvp.client.proxy.PlaceManager;
import org.jboss.as.console.client.domain.model.SimpleCallback;
import org.jboss.as.console.client.shared.dispatch.DispatchAsync;
import org.jboss.as.console.client.shared.dispatch.impl.DMRAction;
import org.jboss.as.console.client.shared.dispatch.impl.DMRResponse;
import org.jboss.ballroom.client.widgets.window.DefaultWindow;
import org.jboss.dmr.client.ModelNode;
import org.jboss.dmr.client.ModelType;
import org.jboss.dmr.client.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.jboss.dmr.client.ModelDescriptionConstants.*;

/**
 * @author Heiko Braun
 * @date 6/15/12
 */
public class BrowserPresenter extends PresenterWidget<BrowserPresenter.MyView> implements StoragePresenter{

    private final PlaceManager placeManager;
    private DispatchAsync dispatcher;
    private boolean hasBeenRevealed;
    private FXStorage storage = new LocalFXStorage();
    private DefaultWindow window;

    public interface MyView extends PopupView {
        void setPresenter(BrowserPresenter presenter);
        void updateChildrenTypes(ModelNode address, List<ModelNode> modelNodes);
        void updateChildrenNames(ModelNode address, List<ModelNode> modelNodes);
        void updateResource(ModelNode address, ModelNode resource);
        void updateDescription(ModelNode address, ModelNode description);

        void setTemplates(Set<FXTemplate> fxTemplates);
    }

    @Inject
    public BrowserPresenter(
            EventBus eventBus, MyView view,
            PlaceManager placeManager, DispatchAsync dispatcher) {
        super(eventBus, view);

        this.placeManager = placeManager;
        this.dispatcher = dispatcher;
    }

    @Override
    protected void onBind() {
        super.onBind();
        getView().setPresenter(this);
    }

    @Override
    protected void onReveal() {
        if(!hasBeenRevealed)
        {
            hasBeenRevealed = true;
            onRefresh();
        }
    }

    @Override
    protected void onHide() {
        placeManager.navigateBack();
    }

    public void readChildrenTypes(final ModelNode address) {

        ModelNode operation  = new ModelNode();
        operation.get(ADDRESS).set(address);
        operation.get(OP).set(READ_CHILDREN_TYPES_OPERATION);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                final ModelNode response = dmrResponse.get();
                getView().updateChildrenTypes(address, response.get(RESULT).asList());
            }
        });
    }

    public void readChildrenNames(final ModelNode address) {

        final List<ModelNode> addressList = address.asList();
        ModelNode typeDenominator = null;
        List<ModelNode> actualAddress = new ArrayList<ModelNode>();
        int i=0;
        for(ModelNode path : addressList)
        {
            if(i<addressList.size()-1)
                actualAddress.add(path);
            else
                typeDenominator = path;

            i++;
        }

        ModelNode operation  = new ModelNode();
        operation.get(ADDRESS).set(actualAddress);
        operation.get(OP).set(READ_CHILDREN_NAMES_OPERATION);
        operation.get(CHILD_TYPE).set(typeDenominator.asProperty().getName());

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {
                final ModelNode response = dmrResponse.get();
                getView().updateChildrenNames(address, response.get(RESULT).asList());
            }
        });

    }

    public void readResource(final ModelNode address) {

        ModelNode operation = new ModelNode();
        operation.get(OP).set(COMPOSITE);
        operation.get(ADDRESS).setEmptyList();


        List<ModelNode> steps = new ArrayList<ModelNode>();

        // the description
        ModelNode descriptionOp  = new ModelNode();
        descriptionOp.get(ADDRESS).set(address);
        descriptionOp.get(OP).set(READ_RESOURCE_DESCRIPTION_OPERATION);
        descriptionOp.get(OPERATIONS).set(true);
        steps.add(descriptionOp);

        // the actual values
        ModelNode resourceOp  = new ModelNode();
        resourceOp.get(ADDRESS).set(address);
        resourceOp.get(OP).set(READ_RESOURCE_OPERATION);
        steps.add(resourceOp);

        operation.get(STEPS).set(steps);

        dispatcher.execute(new DMRAction(operation), new SimpleCallback<DMRResponse>() {
            @Override
            public void onSuccess(DMRResponse dmrResponse) {

                final ModelNode response = dmrResponse.get();
                List<Property> propertyList = response.get(RESULT).asPropertyList();

                for(Property step : propertyList)
                {
                    ModelNode stepResult = step.getValue();
                    if(step.getName().equals("step-1"))
                    {
                        ModelNode desc = null;
                        if(ModelType.LIST.equals(stepResult.get(RESULT).getType()))
                            desc = stepResult.get(RESULT).asList().get(0).get(RESULT).asObject();
                        else
                        {
                            // workaround ...
                            if(!stepResult.hasDefined(RESULT))
                            {
                                Log.error("Undefined element: "+address);
                                desc = new ModelNode();
                            }
                            else
                            {
                                desc = stepResult.get(RESULT).asObject();
                            }
                        }

                        getView().updateDescription(address, desc);
                    }
                    else if(step.getName().equals("step-2"))
                    {
                        getView().updateResource(address, stepResult.get(RESULT).asObject());
                    }
                }
            }}
        );
    }

    public void onRefresh() {
        readChildrenTypes(new ModelNode().setEmptyList());
        getView().setTemplates(storage.loadTemplates());
    }

    // ---------- Storage Presenter  ----

    @Override
    public void launchNewTemplateWizard() {
        window = new DefaultWindow("Create Template");
        window.setWidth(480);
        window.setHeight(420);

        window.trapWidget(
                new FXTemplateWizard(BrowserPresenter.this).asWidget()
        );

        window.setGlassEnabled(true);
        window.center();
    }

    @Override
    public void onRemoveTemplate(String id) {

    }

    @Override
    public void closeDialogue() {
        window.hide();
    }

    @Override
    public void onCreateTemplate(FXTemplate template) {
        System.out.println(template.asModelNode());
    }
}