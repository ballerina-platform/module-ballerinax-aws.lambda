/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinax.awslambda.test;

import org.apache.commons.io.FilenameUtils;
import org.ballerinax.awslambda.test.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

/**
 * Validate that the layer implementation is the same that is used in tests.
 */
public class LayerValidationTest extends BaseTest {
    
    @Test
    public void validateMD5OfLayer() throws IOException, NoSuchAlgorithmException {
        Path ballerinaLayer = Paths.get(FilenameUtils.separatorsToSystem(System.getProperty("ballerina.layer")));
        String ballerinaLayerMD5 = getMD5(ballerinaLayer);
        String testLayerMD5 = getMD5(SOURCE_DIR.resolve("layer-pkg").resolve("ballerina-2-d475e820be")
                .resolve("bootstrap"));
        Assert.assertEquals(ballerinaLayerMD5, testLayerMD5, "Ballerina layers are different in tests.");
        
    }
    
    public static String getMD5(Path filePath) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(Files.readAllBytes(Paths.get(filePath.toString())));
        byte[] digest = md.digest();
        return DatatypeConverter.printHexBinary(digest).toUpperCase();
    }
}
