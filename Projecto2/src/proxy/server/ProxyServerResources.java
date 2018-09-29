package proxy.server;


import com.github.scribejava.apis.TwitterApi;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

import api.Document;
import api.ServerConfig;
import api.rest.IndexerService;

import static javax.ws.rs.core.Response.Status.FORBIDDEN;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;



/**
 * Exemplo de acesso ao servico Twitter.
 * <p>
 * O URL base para programadores esta disponivel em: <br>
 * https://dev.twitter.com/
 * <p>
 * A API REST do sistema esta disponivel em: <br>
 * https://dev.twitter.com/rest/public
 * <p>
 * Para poder aceder ao servico Twitter, deve criar uma app em:
 * https://apps.twitter.com/ onde obtera a apiKey e a apiSecret a usar na
 * criacao do objecto OAuthService. Deve use a opcao: OAuth 2 authorization
 * without a callback URL
 * <p>
 * Este exemplo usa a biblioteca OAuth Scribe, disponivel em:
 * https://github.com/scribejava/scribejava A pagina tem informacao da
 * dependencia Maven que deves adicionar ao teu projeto.
 * <p>
 * e a biblioteca json-simple, disponivel em:
 * http://code.google.com/p/json-simple/ A dependencia Maven esta disponivel em:
 * https://mvnrepository.com/artifact/com.googlecode.json-simple/json-simple
 * <p>
 * e a biblioteca apache commons codec, disponivel em:
 * http://commons.apache.org/proper/commons-codec/
 */

public class ProxyServerResources implements IndexerService
{
	private static String secretValue = ProxyServer.secret;
	private List<String> ids;
	private ServerConfig sConfig;
	private CacheSystem cache;
	@Override
	public List<String> search(String keywords) {
		try {
			if(!cache.hasCached(keywords)){
				System.out.println("Cache Miss");
				final OAuth10aService service = new ServiceBuilder().apiKey(sConfig.getApiKey()).apiSecret(sConfig.getApiSecret())
						.build(TwitterApi.instance());

				final OAuth1AccessToken accessToken = new OAuth1AccessToken( sConfig.getToken(), sConfig.getTokenSecret());


				// Ready to execute operations
				System.out.println("Agora vamos aceder aos followers dum utilizador...");
				OAuthRequest followersReq = new OAuthRequest(Verb.GET,
						"https://api.twitter.com/1.1/search/tweets.json?q="
								+ URLEncoder.encode(keywords, "UTF-8"));
				service.signRequest(accessToken, followersReq);
				final Response followersRes = service.execute(followersReq);
				System.err.println("REST code:" + followersRes.getCode());
				//System.out.println(followersRes.getBody());

				ids = new ArrayList<String>();

				if (followersRes.getCode() == 200){

					JSONParser parser = new JSONParser();
					JSONObject res = (JSONObject) parser.parse(followersRes.getBody());

					JSONArray statuses = (JSONArray) res.get("statuses");
					//System.out.println(statuses);

					
					
					for (int i=0; i<statuses.size(); i++) {

						JSONObject status = (JSONObject) statuses.get(i);
						String str = status.get("id").toString();

						String url = "https://twitter.com/statuses/" + str;

						ids.add(url);

						System.out.println("id " + (i+1) + " > " + ids.get(i));
					}
					
					String[] splitedStr = keywords.split("[ //+]");
					int i=0;
					int hash=0;
					while( i < splitedStr.length ) {
						hash += splitedStr[i].hashCode();
					}
					
					cache.store(hash, ids);
					
				}
				// System.err.println("REST reply:" + followersRes.getBody());
			}
			else{
				System.out.println("Cache hit");
				ids = cache.getList(keywords);
			}
		} catch (InterruptedException | ExecutionException | IOException | ParseException e) {
			e.printStackTrace();
		}
		return ids;
	}

	@Override
	public void configure(String secret, ServerConfig config) {
		// TODO Auto-generated method stub
		sConfig = config;
		System.out.println(sConfig.getToken());
	}

	@Override
	public void add(@PathParam("id") String id, @QueryParam("secret") String secret, Document doc ){
		if(!secret.equals(secretValue)){
			Exception e = new WebApplicationException( FORBIDDEN );
			e.printStackTrace();
		}

	}

	@Override
	public void remove(@PathParam("id") String id, @QueryParam("secret") String secret) {
		if(!secret.equals(secretValue)){
			Exception e = new WebApplicationException( FORBIDDEN );
			e.printStackTrace();
		}

	}
	
	 
}