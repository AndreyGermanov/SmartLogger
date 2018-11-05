package db.adapters;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import utils.DataList;
import utils.DataMap;

import java.util.ArrayList;
import java.util.HashMap;

public class OrientDBDatabaseAdapterTest {

    IDatabaseAdapter adapter = new OrientDBDatabaseAdapter();
    @Before
    public void init() {
        HashMap<String,Object> config = DataMap.create(

                "name", "orientdb_adapter",
                "host", "http://localhost",
                "port", 2480,
                "database", "weather",
                "username", "admin",
                "password", "admin",
                "mode", OrientDBDatabaseAdapter.WorkMode.rest,
                "syslog", DataMap.create("rotateLogs", true,
                        "maxLogFileSize", 2024,
                        "maxLogFiles", 5,
                        "compressArchives", true
                ),
                "collections",DataMap.create(
                        "test", DataMap.create(
                                "name", "test",
                                "idField", "@rid",
                                "fields", DataMap.create(
                                        "@rid",DataMap.create(
                                                "name", "@rid",
                                                "type", "string"
                                        ),
                                        "int_field", DataMap.create(
                                                "name", "int_field",
                                                "type", "integer"
                                        ),
                                        "decimal_field", DataMap.create(
                                                "name", "decimal_field",
                                                "type", "decimal"
                                        ),
                                        "string_field", DataMap.create(
                                                "name", "string_field",
                                                "type", "string"
                                        )
                                )
                        )
                )
        );
        adapter.configure(config);
    }

    @Test
    public void insert() {
        int result = adapter.insert("test", DataList.create(
            DataMap.create("int_field",10,"decimal_field",15.34,"string_field","v1"),
            DataMap.create("int_field",25),
            DataMap.create("string_field","v2")
        ));
        Assert.assertEquals("Should insert all records",3,result);
    }

    @Test
    public void select() {
        ArrayList<HashMap<String,Object>> result = adapter.select("SELECT int_field,string_field FROM test","test");
        Assert.assertNotNull("Should return result",result);
        Assert.assertTrue("Result should contain positive number of rows",result.size()>0);
    }
}
