package api.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import api.Endpoint;

/**
 * Interface do servidor que mantem lista de servidores.
 */
@Path("/contacts")
public interface RendezVousAPI {

	/**
	 * Devolve array com a lista de servidores registados.
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	Endpoint[] endpoints();

	/**
	 * Regista novo servidor.
	 */
	@POST
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	void register(@PathParam("id") String id, Endpoint endpoint);

	/**
	 * De-regista servidor, dado o seu id.
	 */
	@DELETE
	@Path("/{id}")
	void unregister(String id);
}
