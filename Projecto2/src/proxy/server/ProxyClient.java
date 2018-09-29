package proxy.server;

import java.io.IOException;
import java.net.URI;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Response;

import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuth1RequestToken;

import com.github.scribejava.core.oauth.OAuth10aService;

import api.ServerConfig;



public class ProxyClient {

	private static final String HOSTNAME = "192.168.99.1"; //"172.17.0.2";
	private static final int PORT = 8081;

	public static void main(String[] args) {
		try{
			// Substituir pela API key atribuida
			final String apiKey = "EfIX6mrNPIM2yx1ZvaMHhqE0o";
			// Substituir pelo API secret atribuido
			final String apiSecret = "aOSI3olPki3Adsby8ZfPIO0NLbzDpLoM3VXWMIbWfg5qLGN0Aw";

			final OAuth10aService service = new ServiceBuilder().apiKey(apiKey).apiSecret(apiSecret)
					.build(TwitterApi.instance());
			final Scanner in = new Scanner(System.in);

			final OAuth1RequestToken requestToken = service.getRequestToken();
			// Obtain the Authorization URL
			System.out.println("A obter o Authorization URL...");
			final String authorizationUrl = service.getAuthorizationUrl(requestToken);
			System.out.println("Necessario dar permissao neste URL:");
			System.out.println(authorizationUrl);
			System.out.println("e copiar o codigo obtido para aqui:");
			System.out.print(">>");
			final String code = in.nextLine();

			// Trade the Request Token and Verifier for the Access Token
			System.out.println("A obter o Access Token!");
			final OAuth1AccessToken accessToken = service.getAccessToken(requestToken, code);

			ServerConfig sConfig = new ServerConfig(apiKey, apiSecret, accessToken.getToken(), accessToken.getTokenSecret());
			System.out.println(sConfig.getApiKey() + " " + sConfig.getApiSecret() + " " + sConfig.getToken() + " " + sConfig.getTokenSecret());
//			Client client = ClientBuilder.newBuilder().hostnameVerifier(new InsecureHostnameVerifier()).build();
//			
//			URI baseURI = UriBuilder.fromUri("https://" + HOSTNAME + "/").port(PORT).build();
//			
//			WebTarget target = client.target(baseURI);
//			
//			Response response = target.path("/indexer/configure").queryParam("secret", "")
//					.request()
//					.put( Entity.entity( sConfig, MediaType.APPLICATION_JSON));
//			System.out.println(response);
		}
		catch(InterruptedException | ExecutionException | IOException e){
			e.printStackTrace();
		}

	}

//	static public class InsecureHostnameVerifier implements HostnameVerifier {
//		@Override
//		public boolean verify(String hostname, SSLSession session) {
//			return true;
//		}
//	}


}
