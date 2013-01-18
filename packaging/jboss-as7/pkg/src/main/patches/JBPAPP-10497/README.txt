 PATCH NAME:
        JBPAPP-10497
PRODUCT NAME:
        JBoss Enterprise Application Server
VERSION:
        6.0.1
SHORT DESCRIPTION:
        When rewriting soap:address location attributes using the called host ('jbossws.undefined.host' wsdl-host webservices subsystem configuration) in multi-port wsdl, the port corresponding to the current wsdl query is updated only.
LONG DESCRIPTION:
        If WSDL contains more than one service, in some cases it makes sense to update all WSDL services endpoints with absolute URL by ?wsdl request (if services are deployed in the same web application and the same servlet container)
MANUAL INSTALL INSTRUCTIONS:
         Unzip the attached patch (JBPAPP-10497.zip) to a temporary directory.

         Place the JAR's included in the zip to the following locations in your JBoss directory:
                    $JBOSS_HOME/modules/org/jboss/ws/cxf/jbossws-cxf-server/main/jbossws-cxf-server-4.0.6.GA-redhat-2-JBPAPP-10497.jar
                    $JBOSS_HOME/modules/org/apache/cxf/impl/main/cxf-rt-frontend-simple-2.4.9-redhat-2-JBPAPP-10497.jar

        Rename module-jbossws-cxf-server.xml to module.xml and replace the one in the following directory:
                   $JBOSS_HOME/modules/org/jboss/ws/cxf/jbossws-cxf-server/main/module.xml

        Rename module-apache-cxf-main.xml to module.xml and replace the one in the following directory:
                   $JBOSS_HOME/modules/org/apache/cxf/impl/main/module.xml

COMPATIBILITY:
        None
DEPENDENCIES:
        JBoss EAP 6.0.1
SUPERSEDES:
        None
SUPERSEDED BY:
        JBoss EAP 6.0.2
CREATOR:
        Mustafa Musaji
DATE:
        14th December 2012 
