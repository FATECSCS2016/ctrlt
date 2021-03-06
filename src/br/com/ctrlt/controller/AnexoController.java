package br.com.ctrlt.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import br.com.ctrlt.dao.AnexoDAO;
import br.com.ctrlt.dao.TrabalhoDeConclusaoDAO;
import br.com.ctrlt.json.ResponseJson;
import br.com.ctrlt.json.TableResponseJson;
import br.com.ctrlt.model.Anexo;
import br.com.ctrlt.model.TrabalhoDeConclusao;

@Controller
public class AnexoController implements Control<Anexo> {

	@Autowired
    private ServletContext servletContext; 
	
	@Autowired
	private AnexoDAO anexoDAO;
	
	@Autowired
	private TrabalhoDeConclusaoDAO trabalhoDeConclusaoDAO;
	
	@Override
	public String carregarPagina(Model model) {
		return "";
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/cadastra/anexo", method = RequestMethod.POST)
	public ResponseJson cadastrar(@Valid Anexo entidade, BindingResult result) {
		ResponseJson responseJson = new ResponseJson();

		if (!result.hasErrors()) {
			entidade.setAtivo(true);
			
			if (anexoDAO.cadastrar(entidade)) {
				responseJson.setStatus("SUCCESS");
				responseJson.setResult("Anexo cadastrado com sucesso.");
			} else {
				responseJson.setStatus("FAIL");
				responseJson.setResult("Erro ao cadastrar o anexo. Por gentileza contate o administrador do sistema.");
			}
		} else {
			responseJson.setStatus("FAIL");

			String erros = "";

			if (result.getErrorCount() == 1) {
				erros = "O seguinte erro foi apresentado durante a valida��o dos dados: <br />";
			} else {
				erros = "Os seguintes erros foram apresentados durante a valida��o dos dados: <br />";
			}

			for (ObjectError erro : result.getAllErrors()) {
				erros += "<br />" + erro.getDefaultMessage();
			}

			responseJson.setResult(erros);
		}

		return responseJson;
	}
	
	@ResponseBody
	@RequestMapping(value = "rest/cadastra/upload_anexo", method = RequestMethod.POST)
	public ResponseJson uploadAnexo(@RequestParam("anexo") List<MultipartFile> arquivos, @RequestParam("download") List<Boolean> permissao, @RequestParam("id") Long idTcc){
		ResponseJson responseJson = new ResponseJson();
		
		TrabalhoDeConclusao trabalhoDeConclusaoBanco = trabalhoDeConclusaoDAO.pesquisarPorId(idTcc);
		
		//Caminho absoluto do caminho at� a pasta da monografia
		String path = servletContext.getRealPath("/anexos/" + String.valueOf(idTcc) + "/");
		
		if(! arquivos.isEmpty()){
			
			for(int i = 0; i < arquivos.size(); i++){
			
				File file = new File(path);
				
				//Cria o diret�rio caso n�o exista
				if(! file.exists()){
					file.mkdirs();
				}
				
				try {													
					Anexo anexo = new Anexo();
									
					anexo.setCaminho(path);
					anexo.setTamanho(new BigInteger(String.valueOf(arquivos.get(i).getSize())));
					anexo.setNome(arquivos.get(i).getOriginalFilename());
					anexo.setDataUpload(Calendar.getInstance());
					anexo.setNumeroDownloads(0l);
					anexo.setExtensao(FilenameUtils.getExtension(arquivos.get(i).getOriginalFilename()));
					anexo.setVisivel(permissao.get(i));
					anexo.setAtivo(true);
					anexo.setTrabalhoDeConclusao(trabalhoDeConclusaoBanco);
					
					anexoDAO.cadastrar(anexo);
					
					anexo.setCaminho(path + String.valueOf(anexo.getId()) + "\\");
					
					file = new File(path + "/" + String.valueOf(anexo.getId()) + "/");
					
					//Cria o diret�rio caso n�o exista
					if(! file.exists()){
						file.mkdirs();
					}
					
					File arquivoMonografia = new File(path + "/" + String.valueOf(anexo.getId()) + "/" + arquivos.get(i).getOriginalFilename());
					
					arquivos.get(i).transferTo(arquivoMonografia);
					
					trabalhoDeConclusaoBanco.getListaAnexos().add(anexo);							
				} catch (IOException e) {
					responseJson.setStatus("FAIL");
					responseJson.setResult("O anexo " + arquivos.get(i).getName() + " foi carregado, por�m ocorreu um erro ao realizar o upload do arquivo. Por gentileza altere o registro do trabalho"
							+ " de conclus�o e tente realizar o upload novamente.");
				}
			}
			
			if(trabalhoDeConclusaoDAO.alterar(trabalhoDeConclusaoBanco)){
				responseJson.setStatus("SUCCESS");
				if(arquivos.size() > 0){
					responseJson.setResult("Anexo(s) cadastrado(s) com sucesso!");
				}else{
					responseJson.setResult("Anexo cadastrado com sucesso!");
				}
			}else{
				responseJson.setStatus("FAIL");
				if(arquivos.size() > 0){
					responseJson.setResult("O anexo foi carregado, por�m ocorreu um erro ao realizar o upload do arquivo. Por gentileza altere o registro do trabalho"
							+ " de conclus�o e tente realizar o upload novamente.");
				}else{
					responseJson.setResult("Os anexos foram carregados, por�m ocorreu um erro ao realizar o upload dos arquivos. Por gentileza altere o registro do trabalho"
							+ " de conclus�o e tente realizar o upload novamente.");
				}
			}
		}else{
			responseJson.setStatus("FAIL");
			responseJson.setResult("N�o existem anexos a serem enviados para o servidor");
		}
		
		return responseJson;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/altera/anexo", method = RequestMethod.POST)
	public ResponseJson alterar(@Valid Anexo entidade, BindingResult result) {
		ResponseJson responseJson = new ResponseJson();

		if (!result.hasErrors()) {
			if (anexoDAO.alterar(entidade)) {
				responseJson.setStatus("SUCCESS");
				responseJson.setResult("Anexo alterado com sucesso.");
			} else {
				responseJson.setStatus("FAIL");
				responseJson.setResult("Erro ao alterar o anexo. Por gentileza contate o administrador do sistema.");
			}
		} else {
			responseJson.setStatus("FAIL");

			String erros = "";

			if (result.getErrorCount() == 1) {
				erros = "O seguinte erro foi apresentado durante a valida��o dos dados: <br />";
			} else {
				erros = "Os seguintes erros foram apresentados durante a valida��o dos dados: <br />";
			}

			for (ObjectError erro : result.getAllErrors()) {
				erros += "<br />" + erro.getDefaultMessage();
			}

			responseJson.setResult(erros);
		}

		return responseJson;
	}

	@Override
	@ResponseBody
	public TableResponseJson listar(HttpServletRequest req) {
		return null;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/exclui/anexo", method = RequestMethod.POST)
	public ResponseJson excluir(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		ResponseJson responseJson = new ResponseJson();

		// Pega o c�digo do anexo que ser� excluido
		String id = req.getParameter("id");

		// Pega o objeto de anexo para pesquisar no banco
		Anexo anexo = anexoDAO.pesquisarPorId(Integer.parseInt(id));

		// Realiza a exclus�o do anexo
		if (anexoDAO.excluir(anexo)) {
			File arquivo = new File(anexo.getCaminho());
			try {
				FileUtils.deleteDirectory(arquivo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			responseJson.setStatus("SUCCESS");
			responseJson.setResult("Anexo exclu�do com sucesso.");
		} else {
			responseJson.setStatus("FAIL");
			responseJson.setResult("Erro ao excluir o anexo. Por gentileza contate o administrador do sistema.");
		}
		
		return responseJson;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/inativa/anexo", method = RequestMethod.POST)
	public ResponseJson inativar(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		ResponseJson responseJson = new ResponseJson();

		// Pega o c�digo do anexo que ser� inativado
		String id = req.getParameter("id");

		// Pega o objeto de anexo para alterar o status
		Anexo anexo = anexoDAO.pesquisarPorId(Integer.parseInt(id));

		// Altera o status do anexo
		if (anexo.isAtivo()) {
			anexo.setAtivo(false);
		} else {
			anexo.setAtivo(true);
		}

		// Grava as altera��es realizadas com o anexo
		if (anexoDAO.alterar(anexo)) {
			responseJson.setStatus("SUCCESS");

			if (anexo.isAtivo()) {
				responseJson.setResult("Anexo ativado com sucesso.");
			} else {
				responseJson.setResult("Anexo inativado com sucesso.");
			}
		} else {
			responseJson.setStatus("FAIL");
			responseJson.setResult("Erro ao ativar/inativar o anexo. Por gentileza contate o administrador do sistema.");
		}

		return responseJson;
	}
	
	@ResponseBody
	@RequestMapping(value = "rest/status_download/anexo", method = RequestMethod.POST)
	public ResponseJson statusDownload(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		ResponseJson responseJson = new ResponseJson();

		// Pega o c�digo do anexo que ser� inativado
		String id = req.getParameter("id");

		// Pega o objeto de anexo para alterar o status
		Anexo anexo = anexoDAO.pesquisarPorId(Integer.parseInt(id));

		// Altera o status do anexo
		if (anexo.isVisivel()) {
			anexo.setVisivel(false);
		} else {
			anexo.setVisivel(true);
		}

		// Grava as altera��es realizadas com o anexo
		if (anexoDAO.alterar(anexo)) {
			responseJson.setStatus("SUCCESS");

			if (anexo.isVisivel()) {
				responseJson.setResult("Download do anexo habilitado com sucesso.");
			} else {
				responseJson.setResult("Download do anexo bloqueado com sucesso.");
			}
		} else {
			responseJson.setStatus("FAIL");
			responseJson.setResult("Erro ao habilitar/bloquear o download do anexo. Por gentileza contate o administrador do sistema.");
		}

		return responseJson;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/json/anexo", method = RequestMethod.POST)
	public Anexo entidadeJSON(HttpServletRequest req) {
		// Pega o c�digo da Anexo que ser� inativado
		String id = req.getParameter("id");

		// Pega o objeto de anexo para alterar o status
		Anexo anexo = anexoDAO.pesquisarPorId(Integer.parseInt(id));

		return anexo;
	}

	@Override
	public ModelAndView gerarRelatorio(ModelAndView modelAndView) {
		// TODO Auto-generated method stub
		return null;
	}
}
