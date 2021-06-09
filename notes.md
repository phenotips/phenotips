#Pheno deployment

##Pre-Deployment:
- Import pg_dump into remote database
- To be provided!


##Deployment:

1. Maven install
2. Locate war package - Phenotips-***1.4-SNAPSHOT.war
3. Rename to xwiki.war 
4. Locate SOLR data under standalone -> target -> 1.4 Snapshot.zip -> Data -> SOLR
5. (Best guess) Generate hibernate.cfg.xml file with remote DB connection string
6. Replace it inside of WAR package
7. Upload WAR to server location Tomcat/webapps
8. Upload SOLR data to xWiki permanent directory (Needs to be set)
9. Tomcat restart
10. PROFIT!??!
