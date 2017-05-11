/** 
 * Proyecto: Juego de la vida.
 * Resuelve todos los aspectos del almacenamiento del DTO SesionUsuario 
 * utilizando un ArrayList persistente en fichero.
 * Colabora en el patron Fachada.
 * @since: prototipo2.0
 * @source: SesionesDAO.java 
 * @version: 2.1 - 2017.05.11 
 * @author: Alejandro, Judiht, Pedro, Ramón, Jose Maria
 */

package accesoDatos.fichero;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import accesoDatos.DatosException;
import accesoDatos.OperacionesDAO;
import accesoDatos.fichero.UsuariosDAO;
import config.Configuracion;
import modelo.ModeloException;
import modelo.SesionUsuario;

public class SesionesDAO implements OperacionesDAO, Persistente {

	// Requerido por el Singleton.
	private static SesionesDAO instancia = null;

	// Elemento de almacenamiento. 
	private static ArrayList<SesionUsuario> datosSesiones;
	private static File fSesiones;

	/**
	 * Constructor por defecto de uso interno.
	 * Sólo se ejecutará una vez.
	 */
	private SesionesDAO()  {
		datosSesiones = new ArrayList<SesionUsuario>();
		fSesiones = new File(Configuracion.get().getProperty("sesiones.nombreFichero"));
		try {
			recuperarDatos();
		} catch (DatosException e) { }
	}

	/**
	 *  Método estático de acceso a la instancia única.
	 *  Si no existe la crea invocando al constructor interno.
	 *  Utiliza inicialización diferida.
	 *  Sólo se crea una vez; instancia única -patrón singleton-
	 *  @return instancia
	 */
	public static SesionesDAO getInstancia() {
		if (instancia == null) {
			instancia = new SesionesDAO();
		}
		return instancia;
	}

	//OPERACIONES DE PERSISTENCIA.
	/**
	 *  Recupera el Arraylist sesionesUsuario almacenados en fichero. 
	 * @throws DatosException 
	 */
	@Override
	public void recuperarDatos() throws DatosException {
		try {
			if (fSesiones.exists()) {
				FileInputStream fisSesiones = new FileInputStream(fSesiones);
				ObjectInputStream oisSesiones = new ObjectInputStream(fisSesiones);
				datosSesiones = (ArrayList<SesionUsuario>) oisSesiones.readObject();
				oisSesiones.close();
				return;
			}
			throw new DatosException("El fichero de datos: " + fSesiones.getName() + " no existe...");
		} 
		catch (ClassNotFoundException e) {}
		catch (IOException e) {}
	}

	/**
	 *  Cierra datos.
	 */
	@Override
	public void cerrar() {
		guardarDatos();
	}

	/**
	 *  Guarda el Arraylist de sesiones de usuarios en fichero.
	 */
	@Override
	public void guardarDatos() {
		guardarDatos(datosSesiones);
	}

	/**
	 *  Guarda la lista recibida en el fichero de datos.
	 */
	private void guardarDatos(List<SesionUsuario> listaSesiones) {
		try {
			FileOutputStream fosSesiones = new FileOutputStream(fSesiones);
			ObjectOutputStream oosSesiones = new ObjectOutputStream(fosSesiones);
			oosSesiones.writeObject(datosSesiones);		
			oosSesiones.flush();
			oosSesiones.close();
		} 
		catch (IOException e) {}	
	}

	//OPERACIONES DAO
	/**
	 * Búsqueda de sesión por idSesion.
	 * @param idSesion - el idUsr+fecha a buscar.
	 * @return - la sesión encontrada; null si no existe.
	 */
	@Override
	public SesionUsuario obtener(String idSesion)  {
		if (idSesion != null) {
			int posicion = obtenerPosicion(idSesion);			// En base 1
			if (posicion >= 0) {
				return datosSesiones.get(posicion - 1);     	// En base 0
			}
		}
		return null;
	}

	/**
	 *  Obtiene por búsqueda binaria, la posición que ocupa, o ocuparía,  una sesión en 
	 *  la estructura.
	 *	@param idSesion - id de Sesion a buscar.
	 *	@return - la posición, en base 1, que ocupa un objeto o la que ocuparía (negativo).
	 */
	private int obtenerPosicion(String idSesion) {
		int comparacion;
		int inicio = 0;
		int fin = datosSesiones.size() - 1;
		int medio = 0;
		while (inicio <= fin) {
			medio = (inicio + fin) / 2;			// Calcula posición central.
			// Obtiene > 0 si idSesion va después que medio.
			comparacion = idSesion.compareTo(datosSesiones.get(medio).getIdSesion());
			if (comparacion == 0) {			
				return medio + 1;   			// Posción ocupada, base 1	  
			}		
			if (comparacion > 0) {
				inicio = medio + 1;
			}			
			else {
				fin = medio - 1;
			}
		}	
		return -(inicio + 1);					// Posición que ocuparía -negativo- base 1
	}

	/**
	 * Búsqueda de Sesion dado un objeto, reenvía al método que utiliza idSesion.
	 * @param obj - la SesionUsuario a buscar.
	 * @return - la Sesion encontrada; null si no existe.
	 */
	@Override
	public SesionUsuario obtener(Object obj)  {
		return this.obtener(((SesionUsuario) obj).getIdSesion());
	}	

	/**
	 * Búsqueda de todas la sesiones de un mismo usuario.
	 * @param idUsr - el identificador de usuario a buscar.
	 * @return - Sublista con las sesiones encontrada; null si no existe ninguna.
	 */
	public List<SesionUsuario> obtenerTodasMismoUsr(String idUsr)  {
		SesionUsuario aux = null;
		try {
			aux = new SesionUsuario();
		} 
		catch (ModeloException e) { }
		aux.setUsr(UsuariosDAO.getInstancia().obtener(idUsr));
		//Busca posición inserción ordenada por idUsr + fecha. La última para el mismo usuario.
		return separarSesionesUsr(obtenerPosicion(aux.getIdSesion()) - 1);
	}

	/**
	 * Separa en una lista independiente de todas las sesiones de un mismo usuario.
	 * @param ultima - el indice de una sesion almacenada.
	 * @return - Sublista con las sesiones encontrada; null si no existe ninguna.
	 */
	private List<SesionUsuario> separarSesionesUsr(int ultima) {
		// Localiza primera sesión del mismo usuario.
		String idUsr = datosSesiones.get(ultima).getUsr().getIdUsr();
		int primera = ultima;
		for (int i = ultima; i >= 0 && datosSesiones.get(i).getUsr().getIdUsr().equals(idUsr); i--) {
			primera = i;
		}
		// devuelve la sublista de sesiones buscadas.
		return datosSesiones.subList(primera, ultima+1);
	}

	/**
	 * Alta de una nueva SesionUsuario en orden y sin repeticiones según IdUsr + fecha. 
	 * Busca previamente la posición que le corresponde por búsqueda binaria.
	 * @param obj - la SesionUsuario a almacenar.
	 * @throws DatosException - si ya existe.
	 */
	@Override
	public void alta(Object obj) throws DatosException {
		assert obj != null;
		SesionUsuario sesionNueva = (SesionUsuario) obj;							// Para conversión cast
		int posicionInsercion = obtenerPosicion(sesionNueva.getIdSesion()); 
		if (posicionInsercion < 0) {
			datosSesiones.add(-posicionInsercion - 1, sesionNueva); 				// Inserta la sesión en orden.
			return;
		}
		throw new DatosException("(ALTA) La SesionUsuario: " + sesionNueva.getIdSesion() + " ya existe...");
	}

	/**
	 * Elimina el objeto, dado el id utilizado para el almacenamiento.
	 * @param idSesion - identificador de la SesionUsuario a eliminar.
	 * @return - la SesionUsuario eliminada.
	 * @throws DatosException - si no existe.
	 */
	@Override
	public SesionUsuario baja(String idSesion) throws DatosException {
		assert (idSesion != null);
		int posicion = obtenerPosicion(idSesion); 									// En base 1
		if (posicion > 0) {
			return datosSesiones.remove(posicion - 1); 								// En base 0
		}
		throw new DatosException("(BAJA) La SesionUsuario: " + idSesion + " no existe...");
	}

	/**
	 *  Actualiza datos de una SesionUsuario reemplazando el almacenado por el recibido.
	 *	@param obj - SesionUsuario con las modificaciones.
	 *  @throws DatosException - si no existe.
	 */
	@Override
	public void actualizar(Object obj) throws DatosException {
		assert obj != null;
		SesionUsuario sesionActualizada = (SesionUsuario) obj;						// Para conversión cast
		int posicion = obtenerPosicion(sesionActualizada.getIdSesion()); 			// En base 1
		if (posicion > 0) {
			// Reemplaza elemento
			datosSesiones.set(posicion - 1, sesionActualizada);  					// En base 0		
			return;
		}
		throw new DatosException("(ACTUALIZAR) La SesionUsuario: " + sesionActualizada.getIdSesion() + " no existe...");
	}

	/**
	 * Obtiene el listado de todos las sesiones almacenadas.
	 * @return el texto con el volcado de datos.
	 */
	@Override
	public String listarDatos() {
		StringBuilder listado = new StringBuilder();
		for (SesionUsuario sesiones: datosSesiones) {
			if (sesiones != null) {
				listado.append("\n" + sesiones);
			}
		}
		return listado.toString();
	}

	/**
	 * Elimina todos las sesiones almacenadas.
	 */
	@Override
	public void borrarTodo() {
		datosSimulaciones = new ArrayList<Simulacion>();	
	}

}//class
