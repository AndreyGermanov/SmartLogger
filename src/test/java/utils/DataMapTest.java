package utils;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;

public class DataMapTest {

    @Test
    public void createTest() {
        HashMap<String,Object> result = DataMap.create("item1",15,"item2","str");
        Assert.assertEquals("Should contain correct first value",15,result.get("item1"));
        Assert.assertEquals("Should contain correct second value","str",result.get("item2"));

    }
}
