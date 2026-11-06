package skycast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonTest {
    @Test
    void parsesObjectArrayStringAndNumber() {
        Map<String, Object> root = Json.obj(Json.parse(
                "{\"a\": [1, 2.5, \"x\"], \"b\": true, \"c\": null}"));
        assertEquals(List.of(1.0, 2.5, "x"), Json.arr(Json.field(root, "a")));
        assertEquals(Boolean.TRUE, Json.field(root, "b"));
        assertEquals(null, Json.field(root, "c"));
    }

    @Test
    void parsesEscapes() {
        assertEquals("a\"b\nc", Json.parse("\"a\\\"b\\nc\""));
    }

    @Test
    void rejectsTrailingData() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("{} extra"));
    }

    @Test
    void rejectsUnterminatedString() {
        assertThrows(IllegalArgumentException.class, () -> Json.parse("\"abc"));
    }

    @Test
    void missingFieldThrows() {
        Map<String, Object> root = Json.obj(Json.parse("{}"));
        assertThrows(IllegalArgumentException.class, () -> Json.field(root, "nope"));
    }

    @Test
    void emptyObjectAndArray() {
        assertTrue(Json.obj(Json.parse("{}")).isEmpty());
        assertTrue(Json.arr(Json.parse("[]")).isEmpty());
    }
}
