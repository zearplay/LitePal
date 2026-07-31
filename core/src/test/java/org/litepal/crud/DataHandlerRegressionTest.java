package org.litepal.crud;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class DataHandlerRegressionTest {

    @Test
    public void largeIdCollectionUsesFlatInExpression() {
        TestDataHandler handler = new TestDataHandler();
        List<Long> ids = new ArrayList<>();
        for (long id = 1; id <= 1500; id++) {
            ids.add(id);
        }

        String where = handler.whereOfIds(ids);

        assertTrue(where.startsWith("id in (1,2,3"));
        assertTrue(where.endsWith(",1500)"));
        assertFalse(where.contains(" or "));
    }

    @Test
    public void unsupportedGenericSignatureDoesNotCrashMetadataScan() throws Exception {
        TestDataHandler handler = new TestDataHandler();
        Field wildcard = GenericFields.class.getDeclaredField("wildcard");
        Field nested = GenericFields.class.getDeclaredField("nested");

        assertNull(handler.genericType(wildcard));
        assertEquals(List.class, handler.genericType(nested));
    }

    @Test
    public void metadataScanDoesNotRunModelStaticInitializer() {
        TestDataHandler handler = new TestDataHandler();

        List<Field> fields = handler.supportedFields(ExplosiveModel.class.getName());

        assertNotNull(fields);
        assertEquals(1, fields.size());
        assertEquals("name", fields.get(0).getName());
    }

    private static class TestDataHandler extends DataHandler {
        String whereOfIds(List<Long> ids) {
            return getWhereOfIds("id", ids);
        }

        Class<?> genericType(Field field) {
            return getGenericTypeClass(field);
        }

        List<Field> supportedFields(String className) {
            return getSupportedFields(className);
        }
    }

    private static class GenericFields {
        List<? extends String> wildcard;
        List<List<String>> nested;
    }

    public static class ExplosiveModel extends LitePalSupport {
        static {
            if (System.nanoTime() >= 0) {
                throw new AssertionError("Static initializer must not run during metadata scan");
            }
        }

        String name;
    }
}
