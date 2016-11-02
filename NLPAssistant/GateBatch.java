package gate;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.ConsistencyLevel;
import com.datastax.driver.core.QueryOptions;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.TokenAwarePolicy;
import com.google.gson.Gson;
import gate.corpora.DocumentImpl;
import gate.util.InvalidOffsetException;
import gate.util.persistence.PersistenceManager;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import utils.db.DBConnection;

public class GateBatch
{
  private Connection conn;
  private Connection docConn;
  private Session session;
  private Cluster cluster;
  private Session docSession;
  private Cluster docCluster;
  private int batchSize;
  private Map<String, Boolean> annotationTypeMap;
  private boolean allFlag;
  private String dbType = "mysql";
  private String docDBType = "mysql";
  private String rq = "`";
  private java.sql.PreparedStatement pstmt;
  private BoundStatement bstmt;
  private com.datastax.driver.core.PreparedStatement pstmtCheckDocAnnots;
  private Gson gson;
  private List<File> fileList;
  private java.sql.ResultSet rs;
  private com.datastax.driver.core.ResultSet cassRS;
  private File docFile;
  private boolean hasText;
  private int currDoc;
  private int docID;
  private String host;
  private String dbName;
  private String tempDocFolder;
  private String docDBHost;
  private String docDBName;
  private String docNamespace;
  private String docTable;
  private String annotInputTable;
  private String annotOutputTable;
  private String fileRoot;
  private String logFileName;
  private PrintWriter logPW;
  private boolean dbWrite;
  private String provenance;
  private String[] loadAnnotationTypes;
  private String docQuery;
  private int firstFile = 0;
  private File gappFile = null;
  private List annotTypesToWrite = null;
  private String encoding = null;
  private boolean skip = false;
  private boolean verbose;
  
  public void init(String config)
  {
    try
    {
      Properties props = new Properties();
      props.load(new FileReader(config));
      init(props);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public void init(Properties props)
  {
    try
    {
      this.annotationTypeMap = new HashMap();
      this.gson = new Gson();
      
      this.tempDocFolder = props.getProperty("tempDocFolder");
      
      String annotationTypesStr = props.getProperty("annotationTypes");
      if (annotationTypesStr != null)
      {
        String[] annotationTypes = annotationTypesStr.split(",");
        for (int i = 0; i < annotationTypes.length; i++)
        {
          this.annotationTypeMap.put(annotationTypes[i].trim(), Boolean.valueOf(true));
          if (annotationTypes[i].toLowerCase().equals("all")) {
            this.allFlag = true;
          }
        }
      }
      this.dbType = props.getProperty("dbType");
      this.host = props.getProperty("host");
      this.dbName = props.getProperty("dbName");
      
      this.docDBType = props.getProperty("docDBType");
      this.docDBHost = props.getProperty("docDBHost");
      this.docDBName = props.getProperty("docDBName");
      
      this.annotInputTable = props.getProperty("annotationInputTable");
      this.annotOutputTable = props.getProperty("annotationOutputTable");
      
      this.docNamespace = props.getProperty("docNamespace");
      this.docTable = props.getProperty("docTable");
      this.provenance = props.getProperty("provenance");
      this.verbose = Boolean.parseBoolean(props.getProperty("verbose"));
      
      String skipStr = props.getProperty("skip");
      if (skipStr != null) {
        this.skip = Boolean.parseBoolean(skipStr);
      }
      System.setProperty("gate.plugins.home", props.getProperty("gate.plugins.home"));
      System.setProperty("gate.site.config", props.getProperty("gate.site.config"));
      System.setProperty("gate.home", props.getProperty("gate.home"));
      
      this.batchSize = 1000;
      String batchSizeStr = props.getProperty("batchSize");
      if (batchSizeStr != null) {
        this.batchSize = Integer.parseInt(batchSizeStr);
      }
      this.fileRoot = props.getProperty("fileRoot");
      
      this.logFileName = props.getProperty("logFile");
      
      this.logPW = new PrintWriter(new FileWriter(this.logFileName));
      
      String dbWriteStr = props.getProperty("dbWrite");
      this.dbWrite = true;
      if ((dbWriteStr != null) && (dbWriteStr.length() > 0)) {
        this.dbWrite = Boolean.parseBoolean(props.getProperty("dbWrite"));
      }
      String loadAnnotStr = props.getProperty("loadAnnotationTypes");
      this.loadAnnotationTypes = null;
      if ((loadAnnotStr != null) && (loadAnnotStr.length() > 0)) {
        this.loadAnnotationTypes = loadAnnotStr.split(",");
      }
      this.docQuery = props.getProperty("docQuery");
      this.gappFile = new File(props.getProperty("gappFile"));
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  public void process(String user, String password, String docUser, String docPassword)
  {
    try
    {
      this.docFile = new File(this.tempDocFolder + "temp.txt");
      if (this.docDBType.equals("cassandra"))
      {
        this.docCluster = Cluster.builder().addContactPoint(this.docDBHost).withCredentials(docUser, docPassword).withQueryOptions(new QueryOptions().setConsistencyLevel(ConsistencyLevel.ALL)).build();
        this.docSession = this.docCluster.connect(this.docDBName);
        this.cassRS = this.docSession.execute(this.docQuery);
        
        this.rq = "\"";
      }
      else
      {
        this.docConn = DBConnection.dbConnection(docUser, docPassword, this.docDBHost, this.docDBName, this.docDBType);
        Statement stmt = this.docConn.createStatement();
        
        this.rs = stmt.executeQuery(this.docQuery);
      }
      String queryStr = "insert into " + this.annotOutputTable + 
        " (id, document_namespace, document_table, document_id, document_name, annotation_type, start, " + this.rq + "end" + this.rq + ", value, features, provenance) " + 
        "\tvalues (?,?,?,?,?,?,?,?,?,?,?)";
      if (!this.dbType.equals("cassandra"))
      {
        this.conn = DBConnection.dbConnection(user, password, this.host, this.dbName, this.dbType);
        this.conn.setAutoCommit(false);
        this.rq = DBConnection.reservedQuote;
        
        this.pstmt = this.conn.prepareStatement(queryStr);
        
        this.pstmt.setString(2, this.docNamespace);
        this.pstmt.setString(3, this.docTable);
        this.pstmt.setString(11, this.provenance);
      }
      else
      {
        this.cluster = Cluster.builder().addContactPoint(this.host).withCredentials(user, password)
          .withRetryPolicy(DefaultRetryPolicy.INSTANCE)
          .withLoadBalancingPolicy(new TokenAwarePolicy(new DCAwareRoundRobinPolicy()))
          .build();
        
        this.session = this.cluster.connect(this.dbName);
        this.bstmt = new BoundStatement(this.session.prepare(queryStr));
        
        this.pstmtCheckDocAnnots = this.session.prepare("select count(*) from " + this.annotOutputTable + " where document_namespace = '" + this.docNamespace + "' and document_table = '" + this.docTable + 
          "' and document_id = ?");
      }
      Gate.init();
      
      CorpusController application = 
        (CorpusController)PersistenceManager.loadObjectFromFile(this.gappFile);
      
      Corpus corpus = Factory.newCorpus("BatchProcessApp Corpus");
      application.setCorpus(corpus);
      
      this.fileList = new ArrayList();
      if (this.fileRoot != null)
      {
        Path start = Paths.get(this.fileRoot, new String[0]);
        Files.walkFileTree(start, new SimpleFileVisitor()
        {
          public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
            throws IOException
          {
            GateBatch.this.fileList.add(file.toFile());
            return FileVisitResult.CONTINUE;
          }
        });
      }
      int count = 0;
      this.currDoc = 0;
      while (getNextDoc()) {
        if (this.hasText)
        {
          System.out.println("Processing document " + this.docID + "...");
          Document doc = Factory.newDocument(this.docFile.toURI().toURL(), this.encoding);
          DocumentContent docContent = doc.getContent();
          
          boolean loaded = true;
          int maxAnnotID = 0;
          if ((this.skip) && (checkDocumentAnnotations(this.docID) > 0L))
          {
            System.out.println("Skipping...");
          }
          else
          {
            if (this.loadAnnotationTypes != null)
            {
              maxAnnotID = addAnnotationsFromDB(this.docID, doc.getAnnotations(), this.loadAnnotationTypes, this.annotInputTable);
              ((DocumentImpl)doc).setNextAnnotationId(maxAnnotID + 1);
              for (int i = 0; i < this.loadAnnotationTypes.length; i++)
              {
                AnnotationSet loadAnnotSet = doc.getAnnotations().get(this.loadAnnotationTypes[i]);
                if (loadAnnotSet.size() == 0)
                {
                  loaded = false;
                  break;
                }
              }
            }
            if (loaded)
            {
              corpus.add(doc);
              try
              {
                application.execute();
              }
              catch (Exception e)
              {
                e.printStackTrace();
                this.logPW.println("Error in document: " + this.docID);
                this.logPW.println("Message: " + e.getMessage());
                this.logPW.println("\n\n");
                this.logPW.flush();
                System.exit(-1);
              }
              System.out.println("Finished processing Gate...");
              
              AnnotationSet defaultAnnots = doc.getAnnotations();
              Iterator iter = defaultAnnots.iterator();
              while (iter.hasNext())
              {
                Annotation annot = (Annotation)iter.next();
                if ((this.annotationTypeMap.get(annot.getType()) != null) || (this.allFlag))
                {
                  if (this.dbWrite) {
                    insertIntoDB(annot, this.docID, docContent);
                  }
                  count++;
                }
                if ((count % this.batchSize == 0) && 
                  (!this.dbType.equals("cassandra")))
                {
                  this.pstmt.executeBatch();
                  this.conn.commit();
                }
              }
              if (!this.dbType.equals("cassandra"))
              {
                this.pstmt.executeBatch();
                this.conn.commit();
              }
              long check = checkDocumentAnnotations(this.docID);
              if (check != count)
              {
                System.out.println("Missing inserts: " + check + ", " + count);
                System.exit(0);
              }
              count = 0;
              
              corpus.remove(0);
              corpus.unloadDocument(doc);
              corpus.clear();
              
              Factory.deleteResource(doc);
              
              this.currDoc += 1;
            }
          }
        }
      }
      this.logPW.close();
      if (!this.dbType.equals("cassandra"))
      {
        this.pstmt.executeBatch();
        this.conn.commit();
        this.pstmt.close();
        this.conn.close();
      }
      else
      {
        this.session.close();
        this.cluster.close();
      }
      if (!this.docDBType.equals("cassandra"))
      {
        this.docConn.close();
      }
      else
      {
        this.docSession.close();
        this.docCluster.close();
      }
      System.out.println("All done");
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
  
  private boolean getNextDoc()
    throws SQLException, IOException
  {
    if (this.fileList.size() > 0)
    {
      if (this.currDoc < this.fileList.size())
      {
        this.docFile = ((File)this.fileList.get(this.currDoc));
        this.docID = this.currDoc;
        if (this.docFile.getTotalSpace() > 0L) {
          this.hasText = true;
        } else {
          this.hasText = false;
        }
        return true;
      }
    }
    else
    {
      boolean flag = getNextDocFromDB();
      return flag;
    }
    return false;
  }
  
  private boolean getNextDocFromDB()
    throws SQLException, IOException
  {
    String reportText = null;
    boolean flag = false;
    if (!this.docDBType.equals("cassandra"))
    {
      if (this.rs.next())
      {
        this.docID = this.rs.getInt(1);
        reportText = this.rs.getString(2);
        reportText = reportText.replaceAll("\r", "");
        flag = true;
      }
    }
    else if (this.cassRS.iterator().hasNext())
    {
      Row row = (Row)this.cassRS.iterator().next();
      this.docID = row.getInt(0);
      reportText = row.getString(1);
      flag = true;
    }
    if ((reportText != null) && (reportText.length() > 0))
    {
      reportText = reportText.trim();
      PrintWriter pw = new PrintWriter(new FileWriter(this.docFile));
      pw.println(reportText);
      pw.close();
      this.hasText = true;
    }
    else
    {
      this.hasText = false;
    }
    if (this.verbose) {
      System.out.println("Report text: " + reportText);
    }
    return flag;
  }
  
  private int addAnnotationsFromDB(int docID, AnnotationSet annotSet, String[] loadAnnotationTypes, String annotInputTable)
    throws SQLException, InvalidOffsetException
  {
    System.out.println("loading annotations...");
    Statement stmt = this.conn.createStatement();
    StringBuilder strBlder = new StringBuilder("select id, annotation_type, start, " + this.rq + "end" + this.rq + ", features from " + annotInputTable + " where (");
    
    boolean first = true;
    String[] arrayOfString;
    int j = (arrayOfString = loadAnnotationTypes).length;
    for (int i = 0; i < j; i++)
    {
      String annotType = arrayOfString[i];
      if (!first) {
        strBlder.append(" or ");
      } else {
        first = false;
      }
      strBlder.append("annotation_type = '" + annotType + "'");
    }
    strBlder.append(") and document_id = " + docID + " order by id");
    
    java.sql.ResultSet rs = stmt.executeQuery(strBlder.toString());
    int maxAnnotID = -1;
    while (rs.next())
    {
      maxAnnotID = rs.getInt(1);
      FeatureMap fm = Factory.newFeatureMap();
      String features = rs.getString(5);
      Map<Object, Object> fm2 = new HashMap();
      fm2 = (Map)this.gson.fromJson(features, fm2.getClass());
      for (Map.Entry<Object, Object> entry : fm2.entrySet()) {
        fm.put(entry.getKey(), entry.getValue());
      }
      annotSet.add(Integer.valueOf(maxAnnotID), Long.valueOf(rs.getLong(3)), Long.valueOf(rs.getLong(4)), rs.getString(2), fm);
    }
    rs.close();
    stmt.close();
    
    return maxAnnotID;
  }
  
  private void insertIntoDB(Annotation annot, int docID, DocumentContent docContent)
    throws SQLException, InvalidOffsetException
  {
    String annotType = annot.getType();
    long start = annot.getStartNode().getOffset().longValue();
    long end = annot.getEndNode().getOffset().longValue();
    int id = annot.getId().intValue();
    String valueStr = docContent.getContent(Long.valueOf(start), Long.valueOf(end)).toString();
    if (this.verbose) {
      System.out.println("docNamespace: " + this.docNamespace + " docTable: " + this.docTable + " docID: " + docID + " type: " + annotType + 
        " start: " + start + " end: " + end + " id: " + id);
    }
    String featureStr = getFeatureString(annot);
    if (this.dbType.equals("cassandra")) {
      insertIntoCassandra(id, docID, annotType, (int)start, (int)end, valueStr, featureStr);
    } else {
      insertIntoRelational(id, docID, annotType, (int)start, (int)end, valueStr, featureStr);
    }
  }
  
  private void insertIntoRelational(int id, int docID, String annotType, int start, int end, String valueStr, String featureStr)
    throws SQLException
  {
    String docName = this.docNamespace + "-" + this.docTable + "-" + docID;
    this.pstmt.setInt(1, id);
    
    this.pstmt.setInt(4, docID);
    this.pstmt.setString(5, docName);
    this.pstmt.setString(6, annotType);
    this.pstmt.setLong(7, start);
    this.pstmt.setLong(8, end);
    this.pstmt.setString(9, valueStr);
    this.pstmt.setString(10, featureStr);
    this.pstmt.addBatch();
  }
  
  private void insertIntoCassandra(int id, int docID, String annotType, int start, int end, String valueStr, String featureStr)
    throws InvalidOffsetException
  {
    String docName = this.docNamespace + "-" + this.docTable + "-" + docID;
    if (this.verbose) {
      System.out.println(id + ", " + this.docNamespace + ", " + this.docTable + ", " + docID + ", " + docName + ", " + 
        annotType + ", " + start + ", " + end + ", " + valueStr + ", " + featureStr + ", " + this.provenance);
    }
    this.session.execute(this.bstmt.bind(new Object[] { Integer.valueOf(id), this.docNamespace, this.docTable, Integer.valueOf(docID), docName, annotType, Integer.valueOf(start), Integer.valueOf(end), valueStr, featureStr, this.provenance }));
  }
  
  private long checkDocumentAnnotations(int docID)
    throws SQLException
  {
    long count = 0L;
    if (this.dbType.equals("cassandra"))
    {
      com.datastax.driver.core.ResultSet rs = this.session.execute(this.pstmtCheckDocAnnots.bind(new Object[] { Integer.valueOf(docID) }));
      
      Iterator<Row> iter = rs.iterator();
      if (iter.hasNext())
      {
        Row row = (Row)iter.next();
        count = row.getLong(0);
      }
    }
    else
    {
      Statement stmt = this.conn.createStatement();
      java.sql.ResultSet rs = stmt.executeQuery("select count(*) from annotation where document_namespace = '" + this.docNamespace + "' and document_table = '" + this.docTable + "' " + 
        "and document_id = " + docID + " and provenance = '" + this.provenance + "'");
      if (rs.next()) {
        count = rs.getLong(1);
      }
      stmt.close();
    }
    return count;
  }
  
  private String getFeatureString(Annotation annot)
  {
    FeatureMap featureMap = annot.getFeatures();
    StringBuilder strBlder = new StringBuilder("{");
    
    Iterator iter2 = featureMap.keySet().iterator();
    while (iter2.hasNext())
    {
      if (strBlder.length() > 1) {
        strBlder.append(",");
      }
      String key = (String)iter2.next();
      Object obj = featureMap.get(key);
      String objStr = cleanObjString(obj.toString());
      if (!(obj instanceof List))
      {
        strBlder.append("\"" + key + "\":\"" + objStr + "\"");
      }
      else
      {
        boolean first = true;
        StringBuilder listStr = new StringBuilder("\"" + key + "\":[");
        List valueList = (List)obj;
        for (Object value : valueList)
        {
          if (!first) {
            listStr.append(",");
          }
          listStr.append("\"" + value.toString() + "\"");
          first = false;
        }
        listStr.append("]");
        
        strBlder.append(listStr);
      }
    }
    strBlder.append("}");
    
    return strBlder.toString();
  }
  
  private String cleanObjString(String str)
  {
    StringBuilder strBlder = new StringBuilder();
    for (int i = 0; i < str.length(); i++)
    {
      char ch = str.charAt(i);
      if (ch == '"') {
        strBlder.append("\\\"");
      } else if (ch == '\\') {
        strBlder.append("\\\\");
      } else {
        strBlder.append(ch);
      }
    }
    return strBlder.toString();
  }
  
  private Object convertValue(String value)
  {
    Object obj = value;
    boolean converted = false;
    try
    {
      obj = Double.valueOf(Double.parseDouble(value));
      converted = true;
    }
    catch (NumberFormatException localNumberFormatException) {}
    if (!converted) {
      try
      {
        obj = Integer.valueOf(Integer.parseInt(value));
        converted = true;
      }
      catch (NumberFormatException localNumberFormatException1) {}
    }
    if (!converted) {
      obj = value;
    }
    return obj;
  }
  
  public static void main(String[] args)
  {
    if (args.length != 5)
    {
      System.out.println("usage: user password docUser docPassword config");
      System.exit(0);
    }
    GateBatch gateBatch = new GateBatch();
    gateBatch.init(args[4]);
    gateBatch.process(args[0], args[1], args[2], args[3]);
  }
}
