package service;

import java.io.IOException;
import java.security.*;
import java.math.*;
import dao.UsuarioDAO;
import model.Usuario;
import spark.Request;
import spark.Response;

public class UsuarioService {
	private UsuarioDAO usuarioDAO;
	
	
	public UsuarioService() {
		try {
			usuarioDAO = new UsuarioDAO();
			usuarioDAO.conectar();
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}
	
	public boolean unauthorizedCharVerifier(String s) {
		boolean danger = false;
		
		// Verifica se uma string possui aspas simples ou sinal de igual (' e =)
		if (s.indexOf((char) 39) != -1 || s.indexOf('=') != -1) {
			danger = true;
		}
		
		return danger;
	}
	
	public String encryptPassword(String password) throws Exception {
		String encrypted;
		
		MessageDigest m = MessageDigest.getInstance("MD5");
		m.update(password.getBytes(),0,password.length());
		encrypted = new BigInteger(1,m.digest()).toString(16);
		
		return encrypted;
	}

	public Object add(Request request, Response response) {
		String nome = request.queryParams("nome");
		String email = request.queryParams("email");
		String senha = request.queryParams("senha");
		
		Usuario usuarioByName = (Usuario) usuarioDAO.getUsuarioNoPassword(nome);
		Usuario usuarioByEmail = (Usuario) usuarioDAO.getUsuarioNoPassword(email);
		
		if (usuarioByName == null && usuarioByEmail == null) {
			if ( !unauthorizedCharVerifier(nome) && !unauthorizedCharVerifier(email) && !unauthorizedCharVerifier(senha) ) {
				try {
					senha = encryptPassword(senha);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Usuario novo_usuario = new Usuario(nome, senha, email);

				usuarioDAO.add(novo_usuario);
				
				response.status(201); // 201 Created
				return "Usu�rio criado com sucesso!  ---  " + "Nome: " + nome + "  ---  Email: " + email;
			}
			else {
				return "Caracteres proibidos foram encontrados nas informa��es inseridas! --- ' e = (aspas simples e sinal de igual) n�o s�o permitidos por motivos de seguran�a. --- Tente novamente, por favor!";
			}
		} else {
			return "J� existe um usu�rio cadastrado com os dados inseridos. Tente novamente!";
		}
	}

	public Object get(Request request, Response response) {
		String nomeOUemail = request.queryParams("nomeOUemail");
		String senha = request.queryParams("senha");
		
		try {
			senha = encryptPassword(senha);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		Usuario usuario = (Usuario) usuarioDAO.getUsuario(nomeOUemail,senha);
		
		if (usuario != null) {
    	    response.header("Content-Type", "application/xml");
    	    response.header("Content-Encoding", "UTF-8");

            return "<usuario>\n" + 
            		"\t<nome>" + usuario.getNome() + "</nome>\n" +
            		"\t<senha>" + usuario.getSenha() + "</senha>\n" +
            		"\t<email>" + usuario.getEmail() + "</email>\n" +
            		"</site>\n";
        } else {
            //response.status(404); // 404 Not found
            return "Usu�rio '" + nomeOUemail + "' n�o encontrado.";
        }
	}

	public Object update(Request request, Response response) {
		String nomeOUemail = request.queryParams("nomeOUemail");
		String senha = request.queryParams("senha");
		
		try {
			senha = encryptPassword(senha);
		} catch (Exception e) {
			e.printStackTrace();
		}
        
		Usuario usuario = (Usuario) usuarioDAO.getUsuario(nomeOUemail,senha);

        if (usuario != null) {
        	String novo_nome = request.queryParams("novo_nome");
        	String novo_email = request.queryParams("novo_email");
        		
        	if (!novo_nome.equals("")) {
        		usuario.setNome(novo_nome);
        	}
        	if (!novo_email.equals("")) {
        		usuario.setEmail(novo_email);
        	}
        	usuario.setSenha(request.queryParams("nova_senha"));
          	
        	if ( !unauthorizedCharVerifier(usuario.getNome()) && !unauthorizedCharVerifier(usuario.getEmail()) && !unauthorizedCharVerifier(usuario.getSenha()) ) {
        		try {
        			usuario.setSenha(encryptPassword(usuario.getSenha()));
        		} catch (Exception e) {
        			e.printStackTrace();
        		}
        		
        		usuarioDAO.atualizarUsuario(nomeOUemail, senha, usuario);
            	
                return "Informa��es atualizadas com sucesso!";
    		}
    		else {
    			return "Caracteres proibidos foram encontrados nas informa��es inseridas! --- Aspas simples (') e sinal de igual (=) n�o s�o permitidos por motivos de seguran�a. --- Tente novamente, por favor!";
    		}
        	
        } else {
            //response.status(404); // 404 Not found
            return "Usu�rio n�o encontrado ou senha incorreta.";
        }

	}

	public Object remove(Request request, Response response) {
		String nomeOUemail = request.queryParams("nomeOUemail");
		String senha = request.queryParams("senha");
		
		try {
			senha = encryptPassword(senha);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Usuario usuario = (Usuario) usuarioDAO.getUsuario(nomeOUemail,senha);

        if (usuario != null) {

            usuarioDAO.excluirUsuario(nomeOUemail,senha);

            response.status(200); // success
        	return "Conta associada � '" + nomeOUemail + "' foi encerrada.";
        } else {
            //response.status(404); // 404 Not found
            return "N�o foi encontrada uma conta associada � esses dados.";
        }
	}

	public Object getAll(Request request, Response response) {
		StringBuffer returnValue = new StringBuffer("<usuarios type=\"array\">");
		for (Usuario usuario : usuarioDAO.getUsuarios()) {
			returnValue.append("\n<usuario>\n" + 
					"\t<nome>" + usuario.getNome() + "</nome>\n" +
            		"\t<senha>" + usuario.getSenha() + "</senha>\n" +
            		"\t<email>" + usuario.getEmail() + "</email>\n" +
            		"</usuario>\n");
		}
		returnValue.append("</usuarios>");
	    response.header("Content-Type", "application/xml");
	    response.header("Content-Encoding", "UTF-8");
		return returnValue.toString();
	}
}
