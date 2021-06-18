/**
 * ImportAnnotatedFragmentOperation - is an extension of a ro.sync.ecss.extensions.commons.operations.InsertOrReplaceFragmentOperation which adds
 *  a custom operation to the Oxygen XML Editor that lets a user insert an XML fragment (or replace an existing one) that may be built dynamically
 *  from an external resource. It is one of the main classes within the ImportMoreXtension developed at the Digital Academy of the Academy of
 *  Sciences and Literature | Mainz.
 * @author Patrick D. Brookshire
 * @version 1.0.0
 */
package org.adwmainz.da.extensions.importmore.operations;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.adwmainz.da.extensions.askmore.exceptions.InputDialogClosedException;
import org.adwmainz.da.extensions.askmore.models.HashedArgumentsMap;
import org.adwmainz.da.extensions.askmore.models.SelectableOption;
import org.adwmainz.da.extensions.askmore.operations.InsertOrReplaceAnnotatedFragmentOperation;
import org.adwmainz.da.extensions.askmore.utils.ArgumentParser;
import org.adwmainz.da.extensions.askmore.utils.AskMoreArgumentProvider;
import org.adwmainz.da.extensions.askmore.utils.InputDialogUtils;
import org.adwmainz.da.extensions.importmore.exceptions.ImportMoreXMLException;
import org.adwmainz.da.extensions.importmore.exceptions.ImportMoreXPathException;
import org.adwmainz.da.extensions.importmore.utils.ImportMoreAnnotationParser;
import org.adwmainz.da.extensions.importmore.utils.ImportMoreArgumentParser;
import org.adwmainz.da.extensions.importmore.utils.ImportMoreArgumentProvider;
import org.adwmainz.da.extensions.importmore.utils.XMLImportService;

import net.sf.saxon.s9api.XdmNode;
import ro.sync.ecss.extensions.api.ArgumentDescriptor;
import ro.sync.ecss.extensions.api.ArgumentsMap;
import ro.sync.ecss.extensions.api.AuthorAccess;
import ro.sync.ecss.extensions.api.AuthorOperationException;

public class ImportAnnotatedFragmentOperation extends InsertOrReplaceAnnotatedFragmentOperation {
	
	// field
	protected ArgumentDescriptor[] arguments;

	// constructor
	/**
	 * Creates a new ImportAnnotatedFragmentOperation
	 */
	public ImportAnnotatedFragmentOperation() {
		super();

		// get basic arguments from super class
		ArgumentDescriptor[] basicArguments = super.getArguments();

		// load localized data
		ResourceBundle rb = ResourceBundle.getBundle("org.adwmainz.da.extensions.importmore.resources.DialogTextBundle");
		
		// set arguments
		arguments = new ArgumentDescriptor[] {
				ImportMoreArgumentProvider.getResourceLocationArgumentDescriptor(), 
				ImportMoreArgumentProvider.getRequestedElementLocationArgumentDescriptor(),
				ImportMoreArgumentProvider.getNamespacePrefixesArgumentDescriptor(),
				ImportMoreArgumentProvider.getNamespaceUrisArgumentDescriptor(),
				ImportMoreArgumentProvider.getFragmentWithImportExpressionsArgumentDescriptor(),
				ImportMoreArgumentProvider.getSelectableNameExpressionArgumentDescriptor(), 
				AskMoreArgumentProvider.getDialogTitleArgumentDescriptor(rb.getString("IMPORT_ELEMENTS")),
				AskMoreArgumentProvider.getSelectionLabelArgumentDescriptor(rb.getString("CHOOSE_ELEMENT")),
				ImportMoreArgumentProvider.getArgument(basicArguments, AskMoreArgumentProvider.ARGUMENT_INSERT_LOCATION),
				ImportMoreArgumentProvider.getArgument(basicArguments, AskMoreArgumentProvider.ARGUMENT_INSERT_POSITION),
				ImportMoreArgumentProvider.getArgument(basicArguments, AskMoreArgumentProvider.ARGUMENT_GO_TO_NEXT_EDITABLE_POSITION),
				AskMoreArgumentProvider.getRemoveSelectionArgumentDescriptor()
		};
	}
	
	// overridden methods
	@Override
	public String getDescription() {
		return "Extends the default InsertOrReplaceFragmentOperation by adding the possibility to get parts of the fragment from an external resource.";
	}

	@Override
	public void doOperation(AuthorAccess authorAccess, ArgumentsMap args)
			throws IllegalArgumentException, AuthorOperationException {
		// try to get url from an input dialog
		String resourceName;
		try {
			resourceName = ArgumentParser.getValidStringWithUserInput(args, ImportMoreArgumentProvider.ARGUMENT_RESOURCE_LOCATION);
		} catch (InputDialogClosedException ex) {
			// abort action if user closes the dialog
			throw new IllegalArgumentException(AskMoreArgumentProvider.getClosedDialogMessage());
		}
		
		// get other params
		Map<String, String> namespaceMap = ImportMoreArgumentParser.getValidMap(args, ImportMoreArgumentProvider.ARGUMENT_NAMESPACE_PREFIXES, ImportMoreArgumentProvider.ARGUMENT_NAMESPACE_URIS);
		String fragment = ArgumentParser.getValidString(args, AskMoreArgumentProvider.ARGUMENT_FRAGMENT);
		String requestedElementLocation = ArgumentParser.getValidString(args, ImportMoreArgumentProvider.ARGUMENT_REQUESTED_ELEMENT_LOCATION);
		String selectableNameExpression = ArgumentParser.getValidString(args, ImportMoreArgumentProvider.ARGUMENT_SELECTABLE_NAME_EXPRESSION);
		String dialogTitle = ArgumentParser.getValidString(args, AskMoreArgumentProvider.ARGUMENT_DIALOG_TITLE);
		String selectionLabel = ArgumentParser.getValidString(args, AskMoreArgumentProvider.ARGUMENT_SELECTION_LABEL);

		// fetch all options from resource
		Set<SelectableOption<XdmNode>> options;
		try {
			options = XMLImportService.fetchSelectableNodes(resourceName, requestedElementLocation, selectableNameExpression, namespaceMap);
		} catch (IOException | ImportMoreXMLException | ImportMoreXPathException ex) {
			throw new IllegalArgumentException(ex);
		}
		
		// get user selection
		XdmNode selectedBaseNode;
		try {
			selectedBaseNode = InputDialogUtils.fetchSelectedOption(dialogTitle, selectionLabel, options).getRealValue();
		} catch (InputDialogClosedException ex) {
			// abort action if user closes the dialog
			throw new AuthorOperationException(ImportMoreArgumentProvider.getAbortedImportMessage());
		}
		
		// build fragment
		try {
			fragment = ImportMoreAnnotationParser.replaceAnnotations(fragment, selectedBaseNode, namespaceMap);
		} catch (ImportMoreXMLException | ImportMoreXPathException ex) {
			throw new IllegalArgumentException(ex);
		}
		
		// invoke main action from super class
		HashedArgumentsMap parsedArgs = new HashedArgumentsMap(args, Arrays.asList(
				AskMoreArgumentProvider.ARGUMENT_INSERT_LOCATION, 
				AskMoreArgumentProvider.ARGUMENT_INSERT_POSITION, 
				AskMoreArgumentProvider.ARGUMENT_GO_TO_NEXT_EDITABLE_POSITION,
				AskMoreArgumentProvider.ARGUMENT_REMOVE_SELECTION)
		);
		parsedArgs.put(AskMoreArgumentProvider.ARGUMENT_FRAGMENT, fragment);
		
		super.doOperation(authorAccess, parsedArgs);
	}
	
	@Override
	public ArgumentDescriptor[] getArguments() {		
		return arguments;
	}

}
