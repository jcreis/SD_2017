package rest.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import api.Endpoint;

public class RendezVousServer {

	private static Map<String, Long> onIndexers;
	private static RendezVousResources rendezVousResources = new RendezVousResources();

	public static void main(String[] args) throws Exception, IOException {

		onIndexers = new ConcurrentHashMap<String, Long>();

		int port = 8080;
		if( args.length > 0)
			port = Integer.parseInt(args[0]);

		URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(port).build();

		ResourceConfig config = new ResourceConfig();
		config.register( rendezVousResources );

		JdkHttpServerFactory.createHttpServer(baseUri, config);
		System.err.println("REST RendezVousServer ready @ " + baseUri 
				+ " :local IP = " + InetAddress.getLocalHost().getHostAddress());

		// Multicast
		final InetAddress address = InetAddress.getByName("230.0.0.0");
		if( ! address.isMulticastAddress()) {
			System.out.println( "Use range : 224.0.0.0 -- 239.255.255.255");
			System.exit( 1);
		} 

		MulticastSocket socket = new MulticastSocket(9090);
		socket.joinGroup(address);

		URI hostAddress = UriBuilder.fromUri("http://" + InetAddress.getLocalHost().getHostAddress()).port(port).build();



		t1.start();

		while(true){
			byte[] buffer = new byte[65536];
			DatagramPacket pkt = new DatagramPacket(buffer, buffer.length);

			socket.receive(pkt);

			try{ 
				String data = new String(pkt.getData(), 0, pkt.getLength());
				if(data.equals("rendezvous")){

					String str = hostAddress.toString() + "/contacts";
					byte[] url = str.getBytes();
					DatagramPacket packet = new DatagramPacket(url, url.length);

					packet.setAddress(pkt.getAddress());
					packet.setPort(pkt.getPort());

					socket.send(packet);
				}
				else{
					String[] aux = data.split("\\/");
					if(aux[0].equals("alive")){
						//System.out.println("alive");
						onIndexers.put(aux[1], System.currentTimeMillis());
					}
				}
			}
			catch(IOException e){
				System.out.println("Could not send url packet");
			}			
		}
	}





	// heartbeat
	static Thread t1 = new Thread(new Runnable() {			
		public void run() {
			while(true){

				for(String endpointID : onIndexers.keySet()){

					if((System.currentTimeMillis() - onIndexers.get(endpointID)) > 7000){
						onIndexers.remove(endpointID);
						rendezVousResources.unregister(endpointID);
						//System.out.println("removed");
					}
				}
			}
		}

	});
	//		public static int removeIndexer(String removeAddress){
	//			try{
	//	
	//				ClientConfig config = new ClientConfig();
	//				Client client = ClientBuilder.newClient(config);
	//							
	//				URI hostURI = UriBuilder.fromUri("http://" + InetAddress.getLocalHost().getHostAddress()).port(8080).build();
	//				
	//				URI removeURI = UriBuilder.fromUri(removeAddress).build();
	//				System.out.println("removeURI: " + removeURI.toString());
	//				WebTarget target = client.target( hostURI );
	//	
	//				Endpoint endpoint = new Endpoint(removeURI.toString(), Collections.emptyMap());
	//				
	//				Response response = target.path("/contacts/" + endpoint.generateId())
	//						.request()
	//						.delete();
	//				System.out.println(response.getStatus());
	//				return response.getStatus();
	//			}
	//			catch(ProcessingException e){
	//				return 0;
	//			} 
	//			catch (Exception e) {
	//				e.printStackTrace();
	//				return 0;
	//			} 
	//		}

	//	public static List<Endpoint> listEndpoints() throws IllegalArgumentException, UriBuilderException, UnknownHostException{
	//
	//		ClientConfig config = new ClientConfig();
	//		Client client = ClientBuilder.newClient(config);
	//		URI hostAddress = UriBuilder.fromUri("http://" + InetAddress.getLocalHost().getHostAddress()).port(8080).build();
	//		WebTarget target = client.target(hostAddress);
	//		List<Endpoint> endpoints = null;
	//
	//		try{ 
	//			endpoints = target.path("/contacts")
	//					.request()
	//					.accept(MediaType.APPLICATION_JSON)
	//					.get(new GenericType<List<Endpoint>>() {});	
	//		}
	//		catch(ProcessingException e){
	//		}
	//		catch (Exception e) {
	//			e.printStackTrace();
	//		} 
	//		return endpoints;
	//	}
}