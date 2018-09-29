package api.soap;

import javax.jws.WebMethod;
import javax.jws.WebService;

import api.Endpoint;

@WebService
public interface RendezVousAPI {

	final String NAME = "RendezVousService";
	final String NAMESPACE = "http://sd2017";
	final String INTERFACE = "api.soap.RendezVousAPI";
	/**
	 * Devolve array com a lista de servidores de indexacao registados.
	 */
	@WebMethod
	Endpoint[] endpoints();

	/**
	 * Regista novo servidor de indexacao.
	 */
	@WebMethod
	void register(String id, Endpoint endpoint);
	
	/**
	 * De-regista servidor de indexacao, dado o seu id.
	 */
	@WebMethod
	void unregister( String id );
}
