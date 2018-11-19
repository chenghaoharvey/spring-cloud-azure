/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.cloud.config;

import com.microsoft.azure.spring.cloud.config.domain.KeyValueItem;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.spring.cloud.config.TestConstants.VALID_CONN_STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

public class AzureConfigPropertySourceTest {
    private static final String CONTEXT = "/foo/";
    private static final AzureCloudConfigProperties TEST_PROPS = new AzureCloudConfigProperties();
    private static final List<KeyValueItem> TEST_ITEMS = new ArrayList<>();
    private static final String TEST_KEY_1 = "test_key_1";
    private static final String TEST_VALUE_1 = "test_value_1";
    private static final String TEST_KEY_2 = "test_key_2";
    private static final String TEST_VALUE_2 = "test_value_2";
    private static final String TEST_KEY_3 = "test_key_3";
    private static final String TEST_VALUE_3 = "test_value_3";
    private static final KeyValueItem item1 = createItem(CONTEXT, TEST_KEY_1, TEST_VALUE_1);
    private static final KeyValueItem item2 = createItem(CONTEXT, TEST_KEY_2, TEST_VALUE_2);
    private static final KeyValueItem item3 = createItem(CONTEXT, TEST_KEY_3, TEST_VALUE_3);

    private AzureConfigPropertySource propertySource;

    @Mock
    private ConfigServiceOperations operations;

    @BeforeClass
    public static void init() {
        TEST_PROPS.setConnectionString(VALID_CONN_STRING);

        TEST_ITEMS.add(item1);
        TEST_ITEMS.add(item2);
        TEST_ITEMS.add(item3);
    }

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        propertySource = new AzureConfigPropertySource(CONTEXT, TEST_PROPS, operations);
        when(operations.getKeys(anyString(), any())).thenReturn(TEST_ITEMS);
    }

    @Test
    public void testPropCanBeInitAndQueried() {
        propertySource.initProperties();

        String[] keyNames = propertySource.getPropertyNames();
        String[] expectedKeyNames = TEST_ITEMS.stream()
                .map(t -> t.getKey().substring(CONTEXT.length())).toArray(String[]::new);
        assertThat(keyNames).containsExactlyInAnyOrder(expectedKeyNames);

        assertThat(propertySource.getProperty(TEST_KEY_1)).isEqualTo(TEST_VALUE_1);
        assertThat(propertySource.getProperty(TEST_KEY_2)).isEqualTo(TEST_VALUE_2);
        assertThat(propertySource.getProperty(TEST_KEY_3)).isEqualTo(TEST_VALUE_3);
    }

    private static KeyValueItem createItem(String context, String key, String value) {
        KeyValueItem item = new KeyValueItem();
        item.setKey(context + key);
        item.setValue(value);

        return item;
    }
}
