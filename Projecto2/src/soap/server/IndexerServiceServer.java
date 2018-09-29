package soap.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.ws.Endpoint;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;


import api.*;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.server.ResourceConfig;

public class IndexerServiceServer {

	private static String rvUrl;
	private static final int MULTICAST_PORT = 9090;
	private static String secret = "secret";
	
	@SuppressWarnings("restriction")
	public static void main(String[] args) throws Exception {
		int port = 8081;

		if( args.length > 0){
			secret = args[0];
			rvUrl = args[1];
		}
		
		String baseURI = String.format("https://0.0.0.0:%d/indexer", port);

		String hostname = InetAddress.getLocalHost().getHostAddress().toString();

		IndexerServiceResources indexerResources = new IndexerServiceResources();

		HttpsConfigurator configurator = new HttpsConfigurator(SSLContext.getDefault());
		HttpsServer httpsServer = HttpsServer.create(
				new InetSocketAddress("0.0.0.0", port), -1);
		httpsServer.setHttpsConfigurator(configurator);
		HttpContext httpContext = httpsServer.createContext("/contacts");
		httpsServer.start();

		Endpoint ep = Endpoint.create( new IndexerServiceResources());
		ep.publish(httpContext);

		System.err.println("SOAP IndexerServiceServer ready @ " + baseURI 
				+ " :local IP = " + hostname);

		if(rvUrl != null){
			int aux = registerEndpoint(rvUrl);
			if(aux == 204){
				System.out.println(aux + ": endpoint " + rvUrl + "added.");
			}
		}



		final InetAddress inetAddr = InetAddress.getByName("230.0.0.0");
		if( ! inetAddr.isMulticastAddress()) {
			System.out.println( "Use range : 224.0.0.0 -- 239.255.255.255");
			System.exit( 1);
		} 

		MulticastSocket socket = new MulticastSocket() ;

		boolean executed = false;
		for(int i=0; !executed && i<3; i++){
			try{
				socket.setSoTimeout(5000);

				byte[] input = ("rendezvous").getBytes();
				DatagramPacket packet = new DatagramPacket(input, input.length);
				packet.setAddress(inetAddr);
				packet.setPort(MULTICAST_PORT);
				socket.send(packet);

				byte[] buffer = new byte[65536];
				DatagramPacket rvURLPacket = new DatagramPacket(buffer, buffer.length);
				socket.receive(rvURLPacket);
				rvUrl = new String(rvURLPacket.getData(), 0, rvURLPacket.getLength());

				int tmp = registerEndpoint(rvUrl);
				indexerResources.setUrl(rvUrl);
				if(tmp == 204){
					executed = true;
					System.out.println(tmp + ": endpoint " + rvUrl + " added.");
				}

			}
			catch(SocketTimeoutException e) {
				if( i < 2) {
					try { // wait some time
						Thread.sleep( 5000);
					} catch( InterruptedException e1) {
						// do nothing
					}
				}
			}
		}

		t1.start();

	}

	static Thread t1 = new Thread(new Runnable() {			
		public void run() {
			while(true){

				try{

					final InetAddress mcAddress = InetAddress.getByName( "230.0.0.0" ) ;

					if( ! mcAddress.isMulticastAddress()) {
						System.out.println( "Use range : 224.0.0.0 -- 239.255.255.255");
					}
					MulticastSocket socket1 = new MulticastSocket() ;

					try{
						socket1.setSoTimeout(5000);

						URI hostUri = UriBuilder.fromUri("https://" + InetAddress.getLocalHost().getHostAddress()).port(8081).build();
						api.Endpoint endpoint = new api.Endpoint(hostUri.toString(), Collections.emptyMap());
						String endpointID = endpoint.generateId();

						byte[] input = ("alive/" + endpointID).getBytes();
						DatagramPacket heartbeatPacket = new DatagramPacket( input, input.length ) ;
						heartbeatPacket.setAddress( mcAddress ) ;
						heartbeatPacket.setPort( MULTICAST_PORT ) ;
						socket1.send( heartbeatPacket ) ;
					}
					catch(SocketTimeoutException e){
					}
				}
				catch (IOException e1) {
				}

				// Re send every 3 seconds
				try {
					Thread.sleep(3000);
				} 
				catch (InterruptedException e3) {
				}
			}
		}
	});

	public static int registerEndpoint(String rvAddress){
		try{
	
			Client client = ClientBuilder.newBuilder()
					.hostnameVerifier(new InsecureHostnameVerifier())
					.build();

			URI hostURI = UriBuilder.fromUri("https://" + InetAddress.getLocalHost().getHostAddress()).port(8081).build();

			URI rvURI = UriBuilder.fromUri(rvAddress).build();

			WebTarget target = client.target( rvURI );

			HashMap<String, Object> att = new HashMap<String, Object>();
			att.put("type", "soap");
			api.Endpoint endpoint = new api.Endpoint(hostURI.toString(), att);

			String endpointID = endpoint.generateId();

			Response response = target.path("/" + endpointID).queryParam("secret", secret)
					.request()
					.post( Entity.entity( endpoint, MediaType.APPLICATION_JSON));
			System.out.println(response.getStatus());
			return response.getStatus();
		}
		catch(ProcessingException e){
			return 0;
		} 
		catch (Exception e) {

			return 0;
		} 
	}
	static public class InsecureHostnameVerifier implements HostnameVerifier {
		@Override
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	}

}

