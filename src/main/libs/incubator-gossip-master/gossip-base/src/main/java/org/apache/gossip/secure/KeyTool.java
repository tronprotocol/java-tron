/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.gossip.secure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

public class KeyTool {

  public static void generatePubandPrivateKeyFiles(String path, String id) 
          throws NoSuchAlgorithmException, NoSuchProviderException, IOException{
    SecureRandom r = new SecureRandom();
    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
    keyGen.initialize(1024, r);
    KeyPair pair = keyGen.generateKeyPair();
    PrivateKey priv = pair.getPrivate();
    PublicKey pub = pair.getPublic();
    {
      FileOutputStream sigfos = new FileOutputStream(new File(path, id));
      sigfos.write(priv.getEncoded());
      sigfos.close();
    }
    {
      FileOutputStream sigfos = new FileOutputStream(new File(path, id + ".pub"));
      sigfos.write(pub.getEncoded());
      sigfos.close();
    }
  }
  
  public static void main (String [] args) throws 
    NoSuchAlgorithmException, NoSuchProviderException, IOException{
    generatePubandPrivateKeyFiles(args[0], args[1]);
  }
}
