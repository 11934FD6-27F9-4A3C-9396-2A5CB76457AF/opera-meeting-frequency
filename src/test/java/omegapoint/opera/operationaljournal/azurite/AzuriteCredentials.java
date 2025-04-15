package omegapoint.opera.operationaljournal.azurite;

public class AzuriteCredentials {
    // See below link for source on name/key
    // https://learn.microsoft.com/en-us/azure/storage/common/storage-use-azurite?tabs=visual-studio
    public static final String ACCOUNT_NAME = "devstoreaccount1";
    public static final String ACCOUNT_KEY = "Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==";
    private final Host host;
    private final Port port;

    public AzuriteCredentials(final Host host, final Port port) {
        this.host = host;
        this.port = port;
    }

    public String tableConnectionString() {
        return String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;TableEndpoint=http://%s:%s/%s;", ACCOUNT_NAME, ACCOUNT_KEY, host.value, port.value, ACCOUNT_NAME);
    }

    public String queueConnectionString() {
        return String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;QueueEndpoint=http://%s:%s/%s;", ACCOUNT_NAME, ACCOUNT_KEY, host.value, port.value, ACCOUNT_NAME);
    }

    public String blobConnectionString() {
        return String.format("DefaultEndpointsProtocol=http;AccountName=%s;AccountKey=%s;BlobEndpoint=http://%s:%s/%s;", ACCOUNT_NAME, ACCOUNT_KEY, host.value, port.value, ACCOUNT_NAME);
    }
}
