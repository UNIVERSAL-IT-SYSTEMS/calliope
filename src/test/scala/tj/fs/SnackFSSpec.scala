package tj.fs

import org.scalatest.{BeforeAndAfterAll, FlatSpec}
import org.scalatest.matchers.MustMatchers
import org.apache.thrift.async.TAsyncClientManager
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.transport.TNonblockingSocket
import org.apache.cassandra.thrift.Cassandra.AsyncClient
import scala.concurrent.Await
import scala.concurrent.duration._
import tj.util.AsyncUtil
import org.apache.cassandra.thrift.Cassandra.AsyncClient.{set_keyspace_call, system_drop_keyspace_call}
import java.net.URI
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import java.io.{FileNotFoundException, IOException}

class SnackFSSpec extends FlatSpec with BeforeAndAfterAll with MustMatchers {

  val clientManager = new TAsyncClientManager()
  val protocolFactory = new TBinaryProtocol.Factory()
  val transport = new TNonblockingSocket("127.0.0.1", 9160)

  def client = new AsyncClient(protocolFactory, clientManager, transport)

  val store = new ThriftStore(client)

  Await.result(store.createKeyspace(store.buildSchema("FS", 1)), 5 seconds)
  Await.result(AsyncUtil.executeAsync[set_keyspace_call](client.set_keyspace("FS", _)), 5 seconds)

  it should "create a new filesystem with given store" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    fs.getUri must be(uri)
    val user = System.getProperty("user.name", "none")
    fs.getWorkingDirectory must be(new Path("cfs://localhost:9000/user/" + user))
  }

  it should "add a directory" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val result = fs.mkdirs(new Path("/mytestdir"))
    assert(result === true)
  }

  it should "create an entry for a file" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val fsData = fs.create(new Path("/home/shiti/Downloads/JSONParser.js"))
    fsData.write("SOME CONTENT".getBytes)
    val position = fsData.getPos
    position must be(12)
  }

  it should "not when trying to add an existing file as a directory" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val fsData = fs.create(new Path("/home/shiti/Downloads/SOMEFILE"))
    fsData.write("SOME CONTENT".getBytes)
    fsData.close()
    val path = new Path("/home/shiti/Downloads/SOMEFILE")
    fs.mkdirs(path) must be(false)
  }

  it should "allow to read from a file" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val fsData = fs.create(new Path("/home/shiti/Downloads/SOMEFILE"))
    fsData.write("SOME CONTENT".getBytes)
    fsData.close()

    val is = fs.open(new Path("/home/shiti/Downloads/SOMEFILE"))
    var dataArray = new Array[Byte](12)
    is.readFully(0, dataArray)
    is.close()

    val result = new String(dataArray)
    result must be("SOME CONTENT")
  }

  it should "throw an exception when trying to open a directory" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val path = new Path("/test")
    fs.mkdirs(path)
    val exception = intercept[IOException] {
      fs.open(path)
    }
    exception.getMessage must be("Path %s is a directory.".format(path))
  }

  it should "throw an exception when trying to open a file which doesn't exist" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val path = new Path("/newFile")
    val exception = intercept[IOException] {
      fs.open(path)
    }
    exception.getMessage must be("No such file.")
  }

  it should "get file status" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val path = new Path("/home/shiti/Downloads/SOMEFILE")
    val fsData = fs.create(path)
    fsData.write("SOME CONTENT".getBytes)
    fsData.close()

    val status = fs.getFileStatus(path)
    status.isFile must be(true)
    status.getLen must be(12)
    status.getPath must be(path)
  }

  it should "get file block locations" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val path = new Path("/home/shiti/Downloads/SOMEFILE")
    val fsData = fs.create(path)
    fsData.write("This is a test to check the block location details".getBytes)
    fsData.write("This is a test to check the block location details".getBytes)
    fsData.write("This is a test to check the block location details".getBytes)
    fsData.write("This is a test to check the block location details".getBytes)
    fsData.write("This is a test to check the block location details".getBytes)

    fsData.close()

    val status = fs.getFileStatus(path)
    val locations = fs.getFileBlockLocations(status, 0, 10)
    assert(locations(0).getLength === 250)
  }

  it should "list all files/directories within the given directory" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val dirPath1 = new Path("/tmp/user")
    fs.mkdirs(dirPath1)
    val dirPath2 = new Path("/tmp/local")
    fs.mkdirs(dirPath2)

    val filePath1 = new Path("/tmp/testfile")
    val fileData1 = fs.create(filePath1)
    fileData1.write("This is a test to check list functionality".getBytes)
    fileData1.close()

    val filePath2 = new Path("/tmp/user/file")
    val fileData2 = fs.create(filePath2)
    fileData2.write("This is a test to check list functionality".getBytes)
    fileData2.close()

    val baseDirPath = new Path("/tmp")
    val result = fs.listStatus(baseDirPath)
    result.length must be(3)
    result.filter(_.isFile).length must be(1)
    result.filter(_.isDirectory).length must be(2)
  }

  it should "delete all files/directories within the given directory" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())
    val dirPath1 = new Path("/tmp/user")
    fs.mkdirs(dirPath1)
    val dirPath2 = new Path("/tmp/local")
    fs.mkdirs(dirPath2)

    val filePath1 = new Path("/tmp/testfile")
    val fileData1 = fs.create(filePath1)
    fileData1.write("This is a test to check list functionality".getBytes)
    fileData1.close()

    val filePath2 = new Path("/tmp/user/file")
    val fileData2 = fs.create(filePath2)
    fileData2.write("This is a test to check list functionality".getBytes)
    fileData2.close()

    val dirStatus = fs.getFileStatus(dirPath2)
    dirStatus.isDirectory must be(true)

    val baseDirPath = new Path("/tmp")
    val result = fs.delete(baseDirPath, true)
    result must be(true)

    val exception1 = intercept[FileNotFoundException] {
      val dir = fs.getFileStatus(dirPath2)
    }
    exception1.getMessage must be("No such file exists")

    val exception2 = intercept[FileNotFoundException] {
      val dir = fs.getFileStatus(filePath2)
    }
    exception2.getMessage must be("No such file exists")
  }

  it should "rename a file" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())

    val filePath1 = new Path("/tmp/testfile")
    val fileData1 = fs.create(filePath1)
    fileData1.write("This is a test to check rename functionality".getBytes)
    fileData1.close()

    val filePath2 = new Path("/tmp/file")

    val result = fs.rename(filePath1, filePath2)

    result must be(true)

    val exception2 = intercept[FileNotFoundException] {
      val dir = fs.getFileStatus(filePath1)
    }
    exception2.getMessage must be("No such file exists")

    val fileStatus = fs.getFileStatus(filePath2)
    fileStatus.isFile must be(true)
  }

  it should "rename a directory" in {
    val fs = SnackFS(store)
    val uri = URI.create("cfs://localhost:9000")
    fs.initialize(uri, new Configuration())

    val dirPath1 = new Path("/abc/user")
    fs.mkdirs(dirPath1)
    val dirPath2 = new Path("/abc/local")
    fs.mkdirs(dirPath2)

    val filePath1 = new Path("/abc/testfile")
    val fileData1 = fs.create(filePath1)
    fileData1.write("This is a test to check list functionality".getBytes)
    fileData1.close()

    val filePath2 = new Path("/abc/jkl/testfile")
    val fileData2 = fs.create(filePath2)
    fileData2.write("This is a test to check list functionality".getBytes)
    fileData2.close()

    val baseDirPath = new Path("/abc")
    val dirStatus1 = fs.listStatus(new Path("/abc"))
    dirStatus1.filter(_.isFile).length must be(1)

    fs.mkdirs(new Path("/pqr"))
    fs.rename(baseDirPath, new Path("/pqr/lmn"))

    val dirStatus = fs.listStatus(new Path("/pqr/lmn"))
    dirStatus.filter(_.isFile).length must be(1)
    dirStatus.filter(_.isDirectory).length must be(3)

    val fileStatus2 = fs.getFileStatus(new Path("/pqr/lmn/jkl/testfile"))
    fileStatus2.isFile must be(true)
  }

  override def afterAll() = {
    Await.ready(AsyncUtil.executeAsync[system_drop_keyspace_call](client.system_drop_keyspace("FS", _)), 10 seconds)
    clientManager.stop()
  }

}
