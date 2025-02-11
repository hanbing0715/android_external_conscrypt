/* GENERATED SOURCE. DO NOT MODIFY. */
/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.org.conscrypt;

import static com.android.org.conscrypt.HpkeSuite.AEAD_AES_128_GCM;
import static com.android.org.conscrypt.HpkeSuite.AEAD_AES_256_GCM;
import static com.android.org.conscrypt.HpkeSuite.AEAD_CHACHA20POLY1305;
import static com.android.org.conscrypt.HpkeSuite.KDF_HKDF_SHA256;
import static com.android.org.conscrypt.HpkeSuite.KEM_DHKEM_X25519_HKDF_SHA256;
import static com.android.org.conscrypt.TestUtils.decodeHex;
import static com.android.org.conscrypt.TestUtils.encodeHex;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @hide This class is not part of the Android public SDK API
 */
@RunWith(JUnit4.class)
public class HpkeTestVectorsTest {
    private static final String TEST_DATA_ENCRYPTION = "hpke/hpke-encryption.csv";
    private static final String TEST_DATA_EXPORT = "hpke/hpke-export.csv";

    private static final int HPKE_KEM_ID = 0;
    private static final int HPKE_KDF_ID = 1;
    private static final int HPKE_AEAD_ID = 2;
    private static final int HPKE_INFO = 3;
    private static final int HPKE_SECRET_KEY_RECIPIENT = 4;
    private static final int HPKE_SECRET_KEY_EPHEMERAL = 5;
    private static final int HPKE_PUBLIC_KEY_RECIPIENT = 6;
    private static final int HPKE_PUBLIC_KEY_EPHEMERAL = 7;
    private static final int HPKE_AAD = 8;
    private static final int HPKE_CIPHERTEXT = 9;
    private static final int HPKE_PLAINTEXT = 10;
    private static final int HPKE_EXPORTER_CONTEXT = 8;
    private static final int HPKE_L = 9;
    private static final int HPKE_EXPORTED_VALUE = 10;

    private static final Map<String, HpkeSuite> SUPPORTED_HPKE_SUITES = buildSupportedHpkeSuite();

    private static Map<String, HpkeSuite> buildSupportedHpkeSuite() {
        Map<String, HpkeSuite> suiteMap = new HashMap<>();
        suiteMap.put("32:1:1",
                new HpkeSuite(KEM_DHKEM_X25519_HKDF_SHA256, KDF_HKDF_SHA256, AEAD_AES_128_GCM));
        suiteMap.put("32:1:2",
                new HpkeSuite(KEM_DHKEM_X25519_HKDF_SHA256, KDF_HKDF_SHA256, AEAD_AES_256_GCM));
        suiteMap.put("32:1:3",
                new HpkeSuite(
                        KEM_DHKEM_X25519_HKDF_SHA256, KDF_HKDF_SHA256, AEAD_CHACHA20POLY1305));
        return suiteMap;
    }

    @Test
    public void testHpkeBasicEncryption() throws Exception {
        final List<HpkeData> records = getHpkeEncryptionRecords();
        for (HpkeData record : records) {
            testHpkeEncryption(record);
        }
    }

    @Test
    public void testHpkeBasicExport() throws Exception {
        final List<HpkeData> records = getHpkeSecretExportRecords();
        for (HpkeData record : records) {
            testHpkeExport(record);
        }
    }

    private void testHpkeEncryption(HpkeData record) throws Exception {
        final byte[] enc = record.pkEm;

        // Encryption
        final HpkeContextSender contextSender =
                setupBaseForTesting(record.hpkeSuite, record.pkRm, record.info, record.skEm);
        final byte[] encResult = contextSender.getEncapsulated();
        assertArrayEquals("Failed encryption 'enc' " + encodeHex(enc), enc, encResult);
        for (HpkeEncryptionData encryption : record.encryptions) {
            final byte[] ciphertext = contextSender.seal(encryption.pt, encryption.aad);
            assertArrayEquals("Failed encryption 'ciphertext' on data : " + encryption,
                    encryption.ct, ciphertext);
        }

        // Decryption
        final HpkeContextRecipient contextRecipient =
                HpkeContextRecipient.getInstance(record.hpkeSuite.name());
        contextRecipient.init(enc, record.skRm, record.info);
        for (HpkeEncryptionData encryption : record.encryptions) {
            final byte[] plaintext = contextRecipient.open(encryption.ct, encryption.aad);
            assertArrayEquals(
                    "Failed decryption on data : " + encryption, encryption.pt, plaintext);
        }
    }

    private void testHpkeExport(HpkeData record) throws Exception {
        final byte[] enc = record.pkEm;

        // Sender secret export
        final HpkeContextSender contextSender =
                setupBaseForTesting(record.hpkeSuite, record.pkRm, record.info, record.skEm);
        final byte[] encResult = contextSender.getEncapsulated();
        assertArrayEquals("Failed encryption 'enc' " + encodeHex(enc), enc, encResult);
        for (HpkeExporterData exporterData : record.exports) {
            final byte[] export =
                    contextSender.export(exporterData.l, exporterData.exporterContext);
            assertArrayEquals("Failed sender export on data : " + exporterData,
                    exporterData.exportedValue, export);
        }

        // Recipient secret export
        final HpkeContextRecipient contextRecipient =
                HpkeContextRecipient.getInstance(record.hpkeSuite.name());
        contextRecipient.init(enc, record.skRm, record.info);
        for (HpkeExporterData exporterData : record.exports) {
            final byte[] export =
                    contextRecipient.export(exporterData.l, exporterData.exporterContext);
            assertArrayEquals("Failed recipient export on data : " + exporterData,
                    exporterData.exportedValue, export);
        }
    }

    static List<HpkeData> getHpkeEncryptionRecords() throws IOException {
        final List<HpkeData> records = new ArrayList<>();
        final List<String[]> data = TestUtils.readCsvResource(TEST_DATA_ENCRYPTION);

        for (String[] line : data) {
            if (!line[0].isEmpty()) {
                final HpkeData record = new HpkeData();
                record.hpkeSuite =
                        convertSuite(line[HPKE_KEM_ID], line[HPKE_KDF_ID], line[HPKE_AEAD_ID]);
                record.info = TestUtils.decodeHex(line[HPKE_INFO]);
                record.skRm =
                        new OpenSSLX25519PrivateKey(decodeHex(line[HPKE_SECRET_KEY_RECIPIENT]));
                record.skEm = decodeHex(line[HPKE_SECRET_KEY_EPHEMERAL]);
                record.pkRm =
                        new OpenSSLX25519PublicKey(decodeHex(line[HPKE_PUBLIC_KEY_RECIPIENT]));
                record.pkEm = decodeHex(line[HPKE_PUBLIC_KEY_EPHEMERAL]);
                final HpkeEncryptionData encryptionData = new HpkeEncryptionData();
                encryptionData.aad = decodeHex(line[HPKE_AAD]);
                encryptionData.ct = decodeHex(line[HPKE_CIPHERTEXT]);
                encryptionData.pt = decodeHex(line[HPKE_PLAINTEXT]);
                record.encryptions = new ArrayList<>();
                record.encryptions.add(encryptionData);
                records.add(record);
            } else {
                final HpkeEncryptionData encryptionData = new HpkeEncryptionData();
                encryptionData.aad = decodeHex(line[HPKE_AAD]);
                encryptionData.ct = decodeHex(line[HPKE_CIPHERTEXT]);
                encryptionData.pt = decodeHex(line[HPKE_PLAINTEXT]);
                final int lastRecord = records.size() - 1;
                records.get(lastRecord).encryptions.add(encryptionData);
            }
        }
        return records;
    }

    private List<HpkeData> getHpkeSecretExportRecords() throws IOException {
        final List<HpkeData> records = new ArrayList<>();
        final List<String[]> data = TestUtils.readCsvResource(TEST_DATA_EXPORT);
        for (String[] line : data) {
            if (!line[0].isEmpty()) {
                final HpkeData record = new HpkeData();
                record.hpkeSuite =
                        convertSuite(line[HPKE_KEM_ID], line[HPKE_KDF_ID], line[HPKE_AEAD_ID]);
                record.info = decodeHex(line[HPKE_INFO]);
                record.skRm =
                        new OpenSSLX25519PrivateKey(decodeHex(line[HPKE_SECRET_KEY_RECIPIENT]));
                record.skEm = decodeHex(line[HPKE_SECRET_KEY_EPHEMERAL]);
                record.pkRm =
                        new OpenSSLX25519PublicKey(decodeHex(line[HPKE_PUBLIC_KEY_RECIPIENT]));
                record.pkEm = decodeHex(line[HPKE_PUBLIC_KEY_EPHEMERAL]);
                final HpkeExporterData exporterData = new HpkeExporterData();
                exporterData.exporterContext = decodeHex(line[HPKE_EXPORTER_CONTEXT]);
                exporterData.l = Integer.parseInt(line[HPKE_L]);
                exporterData.exportedValue = decodeHex(line[HPKE_EXPORTED_VALUE]);
                record.exports = new ArrayList<>();
                record.exports.add(exporterData);
                records.add(record);
            } else {
                final HpkeExporterData exporterData = new HpkeExporterData();
                exporterData.exporterContext = decodeHex(line[HPKE_EXPORTER_CONTEXT]);
                exporterData.l = Integer.parseInt(line[HPKE_L]);
                exporterData.exportedValue = decodeHex(line[HPKE_EXPORTED_VALUE]);
                final int lastRecord = records.size() - 1;
                records.get(lastRecord).exports.add(exporterData);
            }
        }
        return records;
    }

    private static HpkeSuite convertSuite(String kemId, String kdfId, String aeadId) {
        final String suite = String.join(":", kemId, kdfId, aeadId);

        if (SUPPORTED_HPKE_SUITES.containsKey(suite)) {
            return SUPPORTED_HPKE_SUITES.get(suite);
        }

        throw new IllegalArgumentException("Invalid KEM, KDF, AEAD : " + suite);
    }

    private static HpkeContextSender setupBaseForTesting(
            HpkeSuite suite, PublicKey publicKey, byte[] info, byte[] sKem) throws Exception {
        String algorithm = suite.name();
        HpkeContextSender sender = HpkeContextSender.getInstance(algorithm);
        sender.initForTesting(publicKey, info, sKem);
        return sender;
    }

    static class HpkeData {
        HpkeSuite hpkeSuite;

        byte[] info;
        PrivateKey skRm;
        byte[] skEm;
        PublicKey pkRm;
        byte[] pkEm;
        List<HpkeEncryptionData> encryptions;
        List<HpkeExporterData> exports;

        @Override
        public String toString() {
            return "HpkeData{"
                    + "kem=" + hpkeSuite.getKem() + ", kdf=" + hpkeSuite.getKdf()
                    + ", aead=" + hpkeSuite.getAead() + ", info=" + TestUtils.encodeHex(info)
                    + ", skRm=" + TestUtils.encodeHex(skRm.getEncoded()) + ", skEm="
                    + TestUtils.encodeHex(skEm) + ", pkRm=" + TestUtils.encodeHex(pkRm.getEncoded())
                    + ", pkEm=" + TestUtils.encodeHex(pkEm) + ", encryptions=" + encryptions + '}';
        }
    }

    static class HpkeEncryptionData {
        byte[] aad;
        byte[] ct;
        byte[] pt;

        @Override
        public String toString() {
            return "HpkeEncryptionData{"
                    + "aad=" + TestUtils.encodeHex(aad) + ", ct=" + TestUtils.encodeHex(ct)
                    + ", pt=" + TestUtils.encodeHex(pt) + '}';
        }
    }

    private static class HpkeExporterData {
        byte[] exporterContext;
        int l;
        byte[] exportedValue;

        @Override
        public String toString() {
            return "HpkeExporterData{"
                    + "exporter_context=" + TestUtils.encodeHex(exporterContext) + ", L=" + l
                    + ", exported_value=" + TestUtils.encodeHex(exportedValue) + '}';
        }
    }
}
