<%@page import="java.util.HashMap"%>
<%@page import="com.openpages.sdk.repository.ResourceTypeId"%>
<%@page import="com.openpages.aurora.cache.ContentTypeCache"%>
<%@page import="com.openpages.aurora.common.valueobject.ContentTypeVO"%>
<%@page import="com.openpages.sdk.repository.ResourceSummary"%>
<%@page import="com.openpages.sdk.metadata.MetadataCache"%>
<%@page import="com.openpages.sdk.metadata.ContentType"%>
<%@page import="com.openpages.aurora.service.repository.resource.ResourceNotFoundException"%>
<%@page import="java.util.ArrayList"%>
<%@page import="com.openpages.sdk.trigger.TriggerActionDefinition"%>
<%@page import="com.openpages.aurora.common.logging.LoggerFactory"%>
<%@page import="com.openpages.aurora.util.MiscUtil"%>
<%@page import="org.w3c.dom.NodeList"%>
<%@page import="org.w3c.dom.Element"%>
<%@page import="org.w3c.dom.Document"%>
<%@page import="javax.xml.parsers.DocumentBuilder"%>
<%@page import="javax.xml.parsers.DocumentBuilderFactory"%>
<%@page import="java.io.InputStream"%>
<%@page import="com.openpages.aurora.common.objectid.LabelId"%>
<%@page import="com.openpages.aurora.service.repository.RepositoryServiceManager"%>
<%@page import="com.openpages.sdk.repository.ResourceOptions"%>
<%@page import="com.openpages.aurora.service.repository.storage.Location"%>
<%@page import="com.openpages.sdk.helper.ObjectIdMapper"%>
<%@page import="com.openpages.aurora.common.objectid.VersionId"%>
<%@page import="com.openpages.sdk.repository.RepositoryService"%>
<%@page import="com.openpages.sdk.repository.Resource"%>
<%@page import="com.openpages.sdk.repository.ResourceId"%>
<%@page import="com.openpages.sdk.trigger.TriggerDefinitionCache"%>
<%@page import="com.openpages.sdk.trigger.TriggerDefinition"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="com.openpages.aurora.common.logging.StartupLogger"%>
<%@page import="com.openpages.apps.common.util.HttpContext"%>
<%@page import="com.openpages.sdk.metadata.MetaDataService"%>
<%@page import="com.openpages.sdk.OpenpagesSession"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Reload Trigger</title>
</head>
<body>
<%!
OpenpagesSession opSession = null;
MetaDataService ms = null;
RepositoryService rs = null;
private Map<String, List<TriggerDefinition>> preTriggers;
private Map<String, List<TriggerDefinition>> postTriggers;
%>
<%
opSession = HttpContext.getOpenpagesSession(request);
ms = HttpContext.getMetadataService(request);
rs = HttpContext.getRepositoryService(request);
loadCache();
%>
Done!!!
<%!
protected void loadCache()
throws Exception
{


StartupLogger logger = StartupLogger.getLogger();

logger.info(logger.divider());
try
{

ResourceId resourceId = rs.getResourceId("/_trigger_config_.xml");
Resource resource = rs.getResource(resourceId, new ResourceOptions());

VersionId versionId = ObjectIdMapper.getVOVersionId(resource.getLatestVersion().getVersionId());
RepositoryServiceManager manager = new RepositoryServiceManager(opSession.getAuroraSession().getAccessToken());
com.openpages.aurora.common.objectid.ResourceId id = new com.openpages.aurora.common.objectid.ResourceId(resourceId.getId());
Location location = manager.getLocation(opSession.getAuroraSession().getAccessToken(), id,new LabelId(-1L));

logger.info(logger.semiDivider());
logger.info("Loading configured triggers and their actions ...");
logger.info(logger.semiDivider());

InputStream in = location.getInputStream();

DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
DocumentBuilder builder = factory.newDocumentBuilder();

Document document = builder.parse(in);

Element root = document.getDocumentElement();

NodeList triggerList = root.getElementsByTagName("trigger");
logger.info("Number of triggers = " + triggerList.getLength());
for (int i = 0; i < triggerList.getLength(); i++)
{
Element triggerNode = (Element)triggerList.item(i);


logger.info("");
logger.info("Trigger #" + (i + 1));
String name = triggerNode.getAttribute("name");
logger.info("\tName = " + name);
String operation = triggerNode.getAttribute("operation");
logger.info("\tOperation = " + operation);
String position = triggerNode.getAttribute("position");
logger.info("\tPosition = " + position);

String type = triggerNode.getAttribute("type");
logger.info("\tType = " + type);
String className = null;
if (type.equalsIgnoreCase("custom")) {
className = triggerNode.getAttribute("classname");
}
logger.info("\tClassname = " + className);
if (!isClassValid(className))
{
String msg = "Trigger \"" + name + "\" for \"" + operation + "\" " + "was not loaded. Invalid class \"" + className + "\".";

logger.info("\t" + msg);
LoggerFactory.getLogger().error(msg);
}
else
{
TriggerDefinition def = new TriggerDefinition();

NodeList attrList = triggerNode.getChildNodes();
for (int j = 0; (attrList != null) && (j < attrList.getLength()); j++) {
if ((attrList.item(j) instanceof Element))
{
Element childNode = (Element)attrList.item(j);
if (childNode.getNodeName().equals("attribute"))
{
String attrName = childNode.getAttribute("name");
String attrValue = childNode.getAttribute("value");
logger.info("\t\tAttribute: " + attrName + " = " + attrValue);
def.setAttribute(attrName, attrValue);
}
else if (childNode.getNodeName().equals("actions"))
{
NodeList actionList = childNode.getElementsByTagName("action");
for (int k = 0; (actionList != null) && (k < actionList.getLength()); k++)
{
Element actionNode = (Element)actionList.item(k);

String actionType = actionNode.getAttribute("type");

String actionClass = null;
if (actionType.equalsIgnoreCase("custom")) {
actionClass = actionNode.getAttribute("classname");
}
logger.info("\tAction: " + actionClass + " [" + actionType + "]");
if (!isClassValid(actionClass))
{
String msg = "Action of type \"" + actionType + "\" for trigger \"" + name + "\" " + "was not loaded. Invalid class \"" + actionClass + "\".";

logger.info("\t" + msg);
LoggerFactory.getLogger().error(msg);
}
else
{
TriggerActionDefinition actionDef = new TriggerActionDefinition();
NodeList actionAttrList = actionNode.getElementsByTagName("attribute");
for (int l = 0; (actionAttrList != null) && (l < actionAttrList.getLength()); l++)
{
Element attrNode = (Element)actionAttrList.item(l);

String attrName = attrNode.getAttribute("name");
String attrValue = attrNode.getAttribute("value");
logger.info("\t\tAction Attribute: " + attrName + " = " + attrValue);

actionDef.setAttribute(attrName, attrValue);
}
def.addAction(actionDef);
}
}
}
}
}
preTriggers = new HashMap<String, List<TriggerDefinition>>();
postTriggers = new HashMap<String, List<TriggerDefinition>>();
List<TriggerDefinition> triggers = null;
if (position.equalsIgnoreCase("pre"))
{
triggers = (List)this.preTriggers.get(operation);
if (triggers == null)
{
triggers = new ArrayList();
this.preTriggers.put(operation, triggers);
}
triggers.add(def);
}
else if (position.equalsIgnoreCase("post"))
{
triggers = (List)this.postTriggers.get(operation);
if (triggers == null)
{
triggers = new ArrayList();
this.postTriggers.put(operation, triggers);
}
triggers.add(def);
}
}
}
logger.info("");
TriggerDefinitionCache.getCache().doRefresh(1, triggerList);
}
catch (ResourceNotFoundException rnfe)
{
LoggerFactory.getLogger().error("Error occured : ",rnfe);
}
finally
{
logger.info(logger.divider());
}
}
private static boolean isClassValid(String className)
{
try
{
MiscUtil.loadClass(TriggerDefinitionCache.class, className);
return true;
}
catch (Throwable t)
{
LoggerFactory.getLogger().error("Class: " + className, t);
}
return false;
}
%>
</body>
</html>