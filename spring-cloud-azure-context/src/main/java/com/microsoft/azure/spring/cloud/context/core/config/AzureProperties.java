/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.cloud.context.core.config;

import com.microsoft.azure.spring.cloud.context.core.api.CredentialSupplier;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;

@Getter
@Setter
@ConfigurationProperties("spring.cloud.azure")
public class AzureProperties implements CredentialSupplier {

    private String credentialFilePath;

    private String resourceGroup;

    private String region;

    private boolean autoCreateResources = false;

    @PostConstruct
    public void validate() {
        Assert.hasText(credentialFilePath, "spring.cloud.azure.credential-file-path must be provided");
        Assert.hasText(resourceGroup, "spring.cloud.azure.resource-group must be provided");
        Assert.hasText(region, "spring.cloud.azure.region must be provided");
    }
}