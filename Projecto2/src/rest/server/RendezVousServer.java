package rest.server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class RendezVousServer {

	private static Map<String, Long> onIndexers;
	private static RendezVousResources rendezVousResources = new RendezVousResources();
	public static String secret = "secret";
	
	
	public static void main(String[] args) throws Exception, IOException {

		onIndexers = new ConcurrentHashMap<String, Long>();

		if( args.length > 0){
			secret = args[0];
			
		}
		
		int port = 8080;
	
		URI baseUri = UriBuilder.fromUri("https://0.0.0.0/").port(port).build();

		ResourceConfig config = new ResourceConfig();
		config.register( rendezVousResources );

		JdkHttpServerFactory.createHttpServer(baseUri, config, SSLContext.getDefault());
		System.err.println("SSL REST RendezVousServer ready @ " + baseUri 
				+ " :local IP = " + InetAddress.getLocalHost().getHostAddress());

		// Multicast
		final InetAddress address = InetAddress.getByName("230.0.0.0");
		if( ! address.isMulticastAddress()) {
			System.out.println( "Use range : 224.0.0.0 -- 239.255.255.255");
			System.exit( 1);
		} 

		MulticastSocket socket = new MulticastSocket(9090);
		socket.joinGroup(address);

		URI hostAddress = UriBuilder.fromUri("https://" + InetAddress.getLocalHost().getHostAddress()).port(port).build();



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




	// keep alive
	static Thread t1 = new Thread(new Runnable() {			
		public void run() {
			while(true){

				for(String endpointID : onIndexers.keySet()){

					if((System.currentTimeMillis() - onIndexers.get(endpointID)) > 7000){
						onIndexers.remove(endpointID);
						//rendezVousResources.unregister(endpointID, secret); 
						//System.out.println("removed");
					}
				}
			}
		}

	});

}