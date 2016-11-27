package hu.bme.aut.onlab.rest;

import hu.bme.aut.onlab.model.Member;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Path("/login")
public class LoginRs {

	  @GET
	  @Produces(MediaType.APPLICATION_JSON)
	  public String getLogin(@Context Member member) {
		  JSONObject result = new JSONObject();
		  result.put("success", member != null);
		  if (member == null) {
			  result.put("message", "Invalid username or password!");
		  }
		  return result.toString();
	  }
	  
}
