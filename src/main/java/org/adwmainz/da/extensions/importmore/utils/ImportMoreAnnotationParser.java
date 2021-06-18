/**
 * ImportMoreAnnotationParser.java - is a helper class that provides methods for parsing Strings containing ImportMoreAnnotations like finding them.
 *  It is one of the main classes within the ImportMoreXtension developed at the Digital Academy of the Academy of Sciences and Literature | Mainz.
 * @author Patrick D. Brookshire
 * @version 1.0.0
 */
package org.adwmainz.da.extensions.importmore.utils;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.adwmainz.da.extensions.askmore.utils.RegexUtils;
import org.adwmainz.da.extensions.importmore.exceptions.ImportMoreXMLException;
import org.adwmainz.da.extensions.importmore.exceptions.ImportMoreXPathException;

import net.sf.saxon.s9api.XdmNode;

public class ImportMoreAnnotationParser {
	
	// relevant regex pattern
	protected static final String IMPORT_MORE_ANNOTATION_PATTERN = "\\$\\$IMPORT" + "\\(" + "(.*?)" /* XPath */ + "\\)" + "\\$\\$";
	
	/**
	 * Replaces all ImportMoreAnnotations in a given String with XPath results from the given context node
	 * @param annotatedText a String that may contain serialized annotations
	 * @param contextNode the XdmNode serving as the context for XPath expressions within the given ImportMoreAnnotations
	 * @throws ImportMoreXMLException if an XML parsing error occurrs
	 * @throws ImportMoreXPathException if an XPath parsing error occurrs
	 */
	public static String replaceAnnotations(String annotatedText, XdmNode contextNode, Map<String, String> namespaceMap)
			throws ImportMoreXMLException, ImportMoreXPathException {
		for (String importMoreAnnotation: new HashSet<>(findAnnotations(annotatedText))) {
			String xPathExpression = RegexUtils.getFirstMatch(importMoreAnnotation, IMPORT_MORE_ANNOTATION_PATTERN, 1);
			String importedFragment = SaxonUtils.getFirstXPathResult(contextNode, xPathExpression, namespaceMap).getStringValue();
			annotatedText = annotatedText.replace(importMoreAnnotation, importedFragment);
		}
		return annotatedText;
	}
	
	/**
	 * Returns a List of serialized ImportMoreAnnotations from an annotated String
	 * @param annotatedText a String that may contain serialized annotations 
	 */
	public static List<String> findAnnotations(String annotatedText) {
		return RegexUtils.getMatches(annotatedText, IMPORT_MORE_ANNOTATION_PATTERN);
	}
	
	/**
	 * Describes how to use ImportMoreAnnotations
	 */
	public static String getDescription() {
		return "You may import one or more parts of this argument by using the annotation $$IMPORT( xpath )$$ where xpath denotes an XPath expression relative to"
				+ " the result(s) of the param '" + ImportMoreArgumentProvider.ARGUMENT_REQUESTED_ELEMENT_LOCATION + "'.";
	}
}
