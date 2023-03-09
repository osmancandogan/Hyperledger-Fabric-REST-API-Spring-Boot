package com.example.discidiumbackend;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import org.hyperledger.fabric.client.*;
import org.hyperledger.fabric.client.Status;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
public class AssetService {
    private static final String MSP_ID = System.getenv().getOrDefault("MSP_ID", "Org1MSP");
    private static final String CHANNEL_NAME = System.getenv().getOrDefault("CHANNEL_NAME", "mychannel");
    private static final String CHAINCODE_NAME = System.getenv().getOrDefault("CHAINCODE_NAME", "basic");


    private static final Path CRYPTO_PATH = Paths.get("org1.example.com");
    // Path to user certificate.
    private static final Path CERT_PATH = CRYPTO_PATH.resolve(Paths.get("users/User1@org1.example.com/msp/signcerts/cert.pem"));
    // Path to user private key directory.
    private static final Path KEY_DIR_PATH = CRYPTO_PATH.resolve(Paths.get("users/User1@org1.example.com/msp/keystore"));
    // Path to peer tls certificate.
    private static final Path TLS_CERT_PATH = CRYPTO_PATH.resolve(Paths.get("peers/peer0.org1.example.com/tls/ca.crt"));

    private static final String PEER_ENDPOINT = "localhost:7051";
    private static final String OVERRIDE_AUTH = "peer0.org1.example.com";

    private Contract contract;
    //private final String assetId = "asset" + Instant.now().toEpochMilli();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public AssetService() throws CertificateException, IOException, InvalidKeyException {
        // Get a network instance representing the channel where the smart contract is
        // deployed.
        var channel = newGrpcConnection();

        var builder = Gateway.newInstance().identity(newIdentity()).signer(newSigner()).connection(channel)
                .evaluateOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(15, TimeUnit.SECONDS))
                .submitOptions(options -> options.withDeadlineAfter(5, TimeUnit.SECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(1, TimeUnit.MINUTES));
        try (var gateway = builder.connect()) {
            var network = gateway.getNetwork(CHANNEL_NAME);
            System.out.println("Connected to channel " + CHANNEL_NAME);
            // Get the smart contract from the network.
            this.contract = network.getContract(CHAINCODE_NAME);
            System.out.println("Connected to smart contract " + CHAINCODE_NAME);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }


    private static ManagedChannel newGrpcConnection() throws IOException, CertificateException {
        var tlsCertReader = Files.newBufferedReader(TLS_CERT_PATH);
        var tlsCert = Identities.readX509Certificate(tlsCertReader);

        return NettyChannelBuilder.forTarget(PEER_ENDPOINT)
                .sslContext(GrpcSslContexts.forClient().trustManager(tlsCert).build()).overrideAuthority(OVERRIDE_AUTH)
                .build();
    }

    private static Identity newIdentity() throws IOException, CertificateException {
        var certReader = Files.newBufferedReader(CERT_PATH);
        var certificate = Identities.readX509Certificate(certReader);

        return new X509Identity(MSP_ID, certificate);
    }

    private static Signer newSigner() throws IOException, InvalidKeyException {
        var keyReader = Files.newBufferedReader(getPrivateKeyPath());
        var privateKey = Identities.readPrivateKey(keyReader);

        return Signers.newPrivateKeySigner(privateKey);
    }

    private static Path getPrivateKeyPath() throws IOException {
        try (var keyFiles = Files.list(KEY_DIR_PATH)) {
            return keyFiles.findFirst().orElseThrow();
        }
    }

    String initLedger() throws EndorseException, SubmitException, CommitStatusException {
        System.out.println("\n--> Submit Transaction: InitLedger, function creates the initial set of assets on the ledger");

        SubmittedTransaction commit = contract.newProposal("InitLedger")
                .build()
                .endorse().submitAsync();
        //byte[] result = contract.submitTransaction("InitLedger");
        Status status = commit.getStatus();
        byte[] result = commit.getResult();
        if (!status.isSuccessful()) {
            System.out.println("Error");
        }
        System.out.println("*** Transaction committed successfully");
        System.out.println("Result: " + prettyJson(result));
        System.out.println("Status: " + status);
        return prettyJson(result);
    }

    public String getAllAssets() throws GatewayException {
        System.out.println("\n--> Evaluate Transaction: GetAllAssets, function returns all the current assets on the ledger");

        byte[] result = contract.evaluateTransaction("GetAllAssets");
        return prettyJson(result);
    }

    private String prettyJson(final byte[] json) {
        return prettyJson(new String(json, StandardCharsets.UTF_8));
    }

    private String prettyJson(final String json) {
        var parsedJson = JsonParser.parseString(json);
        return gson.toJson(parsedJson);
    }

    /**
     * Submit a transaction synchronously, blocking until it has been committed to
     * the ledger.
     *
     */
    String createAsset(TemperatureAssetForCatering asset) throws EndorseException, SubmitException, CommitStatusException, CommitException {
        System.out.println("\n--> Submit Transaction: CreateAsset, creates new asset with ID, Color, Size, Owner and AppraisedValue arguments");
        String[] asset_str = new String[]{asset.getAssetId(), String.valueOf(asset.getTemperature()), String.valueOf(asset.getStatus()), asset.getOwner(), "asset" + Instant.now().toEpochMilli()};
        contract.submitTransaction("CreateAsset", asset_str);

        System.out.println("*** Transaction committed successfully");
        return "done";
    }

    /**
     * Submit transaction asynchronously, allowing the application to process the
     * smart contract response (e.g. update a UI) while waiting for the commit
     * notification.
     */
    private void transferAssetAsync(String assetId) throws EndorseException, SubmitException, CommitStatusException {
        System.out.println("\n--> Async Submit Transaction: TransferAsset, updates existing asset owner");

        var commit = contract.newProposal("TransferAsset")
                .addArguments(assetId, "Hospital")
                .build()
                .endorse()
                .submitAsync();

        var result = commit.getResult();
        var oldOwner = new String(result, StandardCharsets.UTF_8);

        System.out.println("*** Successfully submitted transaction to transfer ownership from " + oldOwner + " to Saptha");
        System.out.println("*** Waiting for transaction commit");

        var status = commit.getStatus();
        if (!status.isSuccessful()) {
            throw new RuntimeException("Transaction " + status.getTransactionId() +
                    " failed to commit with status code " + status.getCode());
        }

        System.out.println("*** Transaction committed successfully");
    }

    String readAssetById(String assetID) throws GatewayException {
        System.out.println("\n--> Evaluate Transaction: ReadAsset, function returns asset attributes");

        var evaluateResult = contract.evaluateTransaction("ReadAsset", assetID);

        System.out.println("*** Result:" + prettyJson(evaluateResult));
        return prettyJson(evaluateResult);
    }


    String updateAsset(String assetId, com.example.discidiumbackend.Status status)   {
        try {
            contract.submitTransaction("UpdateAssetStatus", assetId, String.valueOf(status));
            System.out.println("*** Transaction committed successfully");

        } catch (EndorseException | SubmitException | CommitStatusException e) {
            System.out.println("*** Successfully caught the error: ");
            e.printStackTrace(System.out);
            System.out.println("Transaction ID: " + e.getTransactionId());

            var details = e.getDetails();
            if (!details.isEmpty()) {
                System.out.println("Error Details:");
                for (var detail : details) {
                    System.out.println("- address: " + detail.getAddress() + ", mspId: " + detail.getMspId()
                            + ", message: " + detail.getMessage());
                }
            }
        } catch (CommitException e) {
            System.out.println("*** Successfully caught the error: " + e);
            e.printStackTrace(System.out);
            System.out.println("Transaction ID: " + e.getTransactionId());
            System.out.println("Status code: " + e.getCode());
        }
        return "done";
    }

    public String checkTemperature(TemperatureAssetForCatering asset) throws EndorseException, CommitException, SubmitException, CommitStatusException {
        String[] asset_str = new String[]{asset.getAssetId(), String.valueOf(asset.getTemperature()), String.valueOf(asset.getStatus()), asset.getOwner(), "asset" + Instant.now().toEpochMilli()};

        byte[] response = contract.submitTransaction("CheckTemperature", asset_str);
        return prettyJson(response);
    }
}
