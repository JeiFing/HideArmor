package cn.jeifing.hidearmor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginDescriptorTest {
    @Test
    void exposesOnlyTheNewHideCommand() throws IOException {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(input);
            String descriptor = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(descriptor.contains("name: HideArmor"));
            assertTrue(descriptor.contains("version: '1.0.1'"));
            assertTrue(descriptor.contains("main: cn.jeifing.hidearmor.HideArmorPlugin"));
            assertTrue(descriptor.contains("api-version: '26.3'"));
            assertTrue(descriptor.contains("folia-supported: true"));
            assertTrue(descriptor.contains("author: JeiFing"));
            assertTrue(descriptor.contains("permission: hidearmor.use"));
            assertTrue(descriptor.contains("  hide:\n"));
            assertTrue(descriptor.contains("usage: /hide <all none 四位0/1>"));
            assertFalse(descriptor.contains("all|none"));
            assertFalse(descriptor.contains("\n  hidearmor:\n"));
            assertFalse(descriptor.contains("\n  showarmor:\n"));
        }
    }

    @Test
    void embedsTheStandaloneStartupCredit() throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("cn/jeifing/hidearmor/HideArmorPlugin.class")) {
            assertNotNull(input);
            String classBytes = new String(input.readAllBytes(), StandardCharsets.ISO_8859_1);
            assertTrue(classBytes.contains("Made by JeiFing"));
        }
    }

    @Test
    void separatesUsageOptionsWithSpaces() {
        assertEquals("用法：/hide <all none 四位0/1>", ArmorCommand.USAGE_TEXT);
        assertFalse(ArmorCommand.USAGE_TEXT.contains("|"));
    }
}
