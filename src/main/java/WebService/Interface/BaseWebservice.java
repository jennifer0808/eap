package WebService.Interface;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

@WebService
public interface BaseWebservice {
   @WebMethod(action = "http://xxx.com/")
   public String handle(@WebParam(name = "message",targetNamespace="http://Interface.WebService/")String message);
}
