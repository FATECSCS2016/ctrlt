package br.com.ctrlt.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsPdfView;

import br.com.ctrlt.dao.AlunoDAO;
import br.com.ctrlt.dao.AnexoDAO;
import br.com.ctrlt.dao.ProfessorDAO;
import br.com.ctrlt.dao.TrabalhoDeConclusaoDAO;
import br.com.ctrlt.json.ResponseJson;
import br.com.ctrlt.json.ResponseJsonWithId;
import br.com.ctrlt.json.TableResponseJson;
import br.com.ctrlt.model.Anexo;
import br.com.ctrlt.model.Monografia;
import br.com.ctrlt.model.TrabalhoDeConclusao;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Controller
public class TrabalhoDeConclusaoController implements Control<TrabalhoDeConclusao> {

	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
    private ServletContext servletContext; 
	
	@Autowired
	private TrabalhoDeConclusaoDAO trabalhoDeConclusaoDAO;

	@Autowired
	private AlunoDAO alunoDAO;
	
	@Autowired
	private ProfessorDAO professorDAO;
	
	@Autowired
	private AnexoDAO anexoDAO;
	
	@Override
	@RequestMapping(value = "adm/cadastro/trabalho_de_conclusao", method = RequestMethod.GET)
	public String carregarPagina(Model model) {
		model.addAttribute("alunos", alunoDAO.listar(" WHERE a.ativo = true"));
		model.addAttribute("professores", professorDAO.listar(" WHERE p.ativo = true"));
		
		return "adm/cadastros/cadastro_trabalho_de_conclusao";
	}
	
	@RequestMapping(value = "trabalho_de_conclusao/{id}", method = RequestMethod.GET)
	public String carregarTrabalhoDeConclusao(@PathVariable("id") Long idTcc, Model model) {
		TrabalhoDeConclusao trabalhoDeConclusao = trabalhoDeConclusaoDAO.pesquisarPorId(idTcc);
		
		model.addAttribute("trabalhoDeConclusao", trabalhoDeConclusao);
		
		return "gallery/trabalho_de_conclusao";
	}

	@Override
	public ResponseJson cadastrar(@Valid TrabalhoDeConclusao entidade, BindingResult result) {
		return null;
	}
	
	@ResponseBody
	@RequestMapping(value = "rest/cadastra/trabalho_de_conclusao", method = RequestMethod.POST)
	public ResponseJsonWithId cadastrarComRetornoDeId(@Valid TrabalhoDeConclusao entidade, BindingResult result) {
		ResponseJsonWithId responseJsonWithId = new ResponseJsonWithId();

		//Seta a data de publica��o
		entidade.setDataPublicacao(Calendar.getInstance());
		
		if(entidade.getListaProfessores() != null){
			//Atualiza a lista de Professores
			for(int i = 0; i < entidade.getListaProfessores().size(); i++){
				if (entidade.getListaProfessores().get(i).getId() == 0){
					entidade.getListaProfessores().remove(i);
					i = 0;
				}else{
					entidade.getListaProfessores().set(i, professorDAO.pesquisarPorId(entidade.getListaProfessores().get(i).getId()));
				}
			}
		}
		
		String validacaoAlunos = "";
		
		if(entidade.getListaAlunos() != null){
			//Atualiza a lista de alunos
			for(int i = 0; i < entidade.getListaAlunos().size(); i++){
				if (entidade.getListaAlunos().get(i).getId() != 0){
					entidade.getListaAlunos().set(i, alunoDAO.pesquisarPorId(entidade.getListaAlunos().get(i).getId()));
					
					if(entidade.getListaAlunos().get(i).getTrabalhoDeConclusao() != null){
						validacaoAlunos += "<br />Aluno " + entidade.getListaAlunos().get(i).getNome() + " j� est� associado a outro trabalho de conclus�o";
					}
				}
			}
		}
		
		if(! validacaoAlunos.isEmpty()){
			responseJsonWithId.setStatus("FAIL");
			responseJsonWithId.setResult("O seguinte erro foi apresentado durante a valida��o dos dados: <br />" + validacaoAlunos);
			responseJsonWithId.setId(0l);
			
			return responseJsonWithId;
		}
		
		if (!result.hasErrors()) {
			entidade.setAtivo(false);

			if (trabalhoDeConclusaoDAO.cadastrar(entidade)) {
				
				if(entidade.getListaAlunos() != null){
					//Atualiza a lista de alunos
					for(int i = 0; i < entidade.getListaAlunos().size(); i++){
						if (entidade.getListaAlunos().get(i).getId() == 0){
							entidade.getListaAlunos().remove(i);
							i = 0;
						}else{
							entidade.getListaAlunos().set(i, alunoDAO.pesquisarPorId(entidade.getListaAlunos().get(i).getId()));
							entidade.getListaAlunos().get(i).setTrabalhoDeConclusao(entidade);
						}
						entidade.getListaAlunos().get(i).setTrabalhoDeConclusao(entidade);
					}
				}
				
				trabalhoDeConclusaoDAO.alterar(entidade);
				
				responseJsonWithId.setStatus("SUCCESS");
				responseJsonWithId.setResult("Trabalho de conclus�o cadastrado com sucesso.");
				responseJsonWithId.setId(entidade.getId());
			} else {
				responseJsonWithId.setStatus("FAIL");
				responseJsonWithId.setResult("Erro ao cadastrar o trabalho de conclus�o. Por gentileza contate o administrador do sistema.");
				responseJsonWithId.setId(0l);
			}
		} else {
			responseJsonWithId.setStatus("FAIL");

			String erros = "";

			if (result.getErrorCount() == 1) {
				erros = "O seguinte erro foi apresentado durante a valida��o dos dados: <br />";
			} else {
				erros = "Os seguintes erros foram apresentados durante a valida��o dos dados: <br />";
			}

			for (ObjectError erro : result.getAllErrors()) {
				erros += "<br />" + erro.getDefaultMessage();
			}

			responseJsonWithId.setResult(erros);
		}

		return responseJsonWithId;
	}
	
	@ResponseBody
	@RequestMapping(value = "rest/cadastra/upload_monografia", method = RequestMethod.POST)
	public ResponseJson uploadMonografia(@RequestParam("monografia") MultipartFile arquivo, @RequestParam("id") Long idTcc){
		ResponseJson responseJson = new ResponseJson();
		
		TrabalhoDeConclusao trabalhoDeConclusaoBanco = trabalhoDeConclusaoDAO.pesquisarPorId(idTcc);
		
		if(! arquivo.isEmpty()){
			if(trabalhoDeConclusaoBanco.getMonografia() != null){
				//Exclui o arquivo de monografia ao excluir o Trabalho de Conclus�o
				File monografia = new File(trabalhoDeConclusaoBanco.getMonografia().getCaminho());
				try {
					FileUtils.deleteDirectory(monografia);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			//Caminho absoluto do caminho at� a pasta da monografia
			String path = servletContext.getRealPath("/monografias/" + String.valueOf(idTcc) + "/");
			
			File file = new File(path);
			
			//Cria o diret�rio caso n�o exista
			if(! file.exists()){
				file.mkdirs();
			}
			
			try {			
				File arquivoMonografia = new File(path + arquivo.getOriginalFilename());
				
				arquivo.transferTo(arquivoMonografia);
				
				Monografia monografia = trabalhoDeConclusaoBanco.getMonografia();
				
				if(trabalhoDeConclusaoBanco.getMonografia() == null){
					monografia = new Monografia();
				}
				
				monografia.setCaminho(path);
				monografia.setTamanho(new BigInteger(String.valueOf(arquivo.getSize())));
				monografia.setNome(arquivoMonografia.getName());
				monografia.setDataUpload(Calendar.getInstance());
				monografia.setNumeroDownloads(0l);
				monografia.setExtensao(FilenameUtils.getExtension(arquivoMonografia.getName()));
				monografia.setAtivo(true);
				
				trabalhoDeConclusaoBanco.setMonografia(monografia);
				
				if(trabalhoDeConclusaoDAO.alterar(trabalhoDeConclusaoBanco)){
					responseJson.setStatus("SUCCESS");
					responseJson.setResult("Trabalho de conclus�o de curso cadastrado com sucesso!");
				}else{
					responseJson.setStatus("FAIL");
					responseJson.setResult("A monografia foi carregada, por�m ocorreu um erro ao realizar o upload do arquivo. Por gentileza altere o registro do trabalho"
							+ " de conclus�o e tente realizar o upload novamente.");
				}
				
				
				
			} catch (IOException e) {
				responseJson.setStatus("FAIL");
				responseJson.setResult("A monografia foi carregada, por�m ocorreu um erro ao realizar o upload do arquivo. Por gentileza altere o registro do trabalho"
						+ " de conclus�o e tente realizar o upload novamente.");
			}
		}else{
			responseJson.setStatus("FAIL");
			responseJson.setResult("A monografia foi carregada, por�m ocorreu um erro ao realizar o upload do arquivo. Por gentileza altere o registro do trabalho"
					+ " de conclus�o e tente realizar o upload novamente.");
		}
		
		return responseJson;
	}

	@Override
	@ResponseBody
	public ResponseJson alterar(@Valid TrabalhoDeConclusao entidade, BindingResult result) {
		return null;
	}
	
	@ResponseBody
	@RequestMapping(value = "rest/altera/trabalho_de_conclusao", method = RequestMethod.POST)
	public ResponseJsonWithId alteraComRetornoDeId(@Valid TrabalhoDeConclusao entidade, BindingResult result) {
		ResponseJsonWithId responseJsonWithId = new ResponseJsonWithId();
		
		TrabalhoDeConclusao trabalhoDeConclusaoBanco = trabalhoDeConclusaoDAO.pesquisarPorId(entidade.getId());
		
		String validacaoAlunos = "";
		
		if(entidade.getListaAlunos() != null){
			//Atualiza a lista de alunos
			for(int i = 0; i < entidade.getListaAlunos().size(); i++){
				if (entidade.getListaAlunos().get(i).getId() != 0){
					entidade.getListaAlunos().set(i, alunoDAO.pesquisarPorId(entidade.getListaAlunos().get(i).getId()));
					
					if(entidade.getListaAlunos().get(i).getTrabalhoDeConclusao() != null){
						if(entidade.getListaAlunos().get(i).getTrabalhoDeConclusao().getId() != trabalhoDeConclusaoBanco.getId()){
							validacaoAlunos += "<br />Aluno " + entidade.getListaAlunos().get(i).getNome() + " j� est� associado a outro trabalho de conclus�o";
						}
					}
				}
			}
		}
		
		if(! validacaoAlunos.isEmpty()){
			responseJsonWithId.setStatus("FAIL");
			responseJsonWithId.setResult("O seguinte erro foi apresentado durante a valida��o dos dados: <br />" + validacaoAlunos);
			responseJsonWithId.setId(0l);
			
			return responseJsonWithId;
		}

		//Remove as associa��es dos Alunos j� associados ao trabalho de conclus�o
		if(trabalhoDeConclusaoBanco.getListaAlunos() != null){
			for(int i = 0; i < trabalhoDeConclusaoBanco.getListaAlunos().size(); i++){
				trabalhoDeConclusaoBanco.getListaAlunos().get(i).setTrabalhoDeConclusao(null);
			}
		}
		
		trabalhoDeConclusaoDAO.alterar(trabalhoDeConclusaoBanco);
		
		entidade.setDataPublicacao(trabalhoDeConclusaoBanco.getDataPublicacao());
		entidade.setMonografia(trabalhoDeConclusaoBanco.getMonografia());
		entidade.setListaAnexos(trabalhoDeConclusaoBanco.getListaAnexos());
		
		if(entidade.getListaProfessores() != null){
			//Atualiza a lista de Professores
			for(int i = 0; i < entidade.getListaProfessores().size(); i++){
				if (entidade.getListaProfessores().get(i).getId() == 0){
					entidade.getListaProfessores().remove(i);
					i = 0;
				}else{
					entidade.getListaProfessores().set(i, professorDAO.pesquisarPorId(entidade.getListaProfessores().get(i).getId()));
				}
			}
		}
		
		if(entidade.getListaAlunos() != null){
			//Atualiza a lista de Alunos
			for(int i = 0; i < entidade.getListaAlunos().size(); i++){
				if (entidade.getListaAlunos().get(i).getId() == 0){
					entidade.getListaAlunos().remove(i);
					i = 0;
				}else{
					entidade.getListaAlunos().set(i, alunoDAO.pesquisarPorId(entidade.getListaAlunos().get(i).getId()));
					entidade.getListaAlunos().get(i).setTrabalhoDeConclusao(entidade);
				}
			}
		}
		
		if (!result.hasErrors()) {
			entidade.setAtivo(trabalhoDeConclusaoBanco.isAtivo());
			
			if (trabalhoDeConclusaoDAO.alterar(entidade)) {				
				responseJsonWithId.setStatus("SUCCESS");
				responseJsonWithId.setResult("Trabalho de conclus�o alterado com sucesso.");
				responseJsonWithId.setId(entidade.getId());
			} else {
				responseJsonWithId.setStatus("FAIL");
				responseJsonWithId.setResult("Erro ao cadastrar o trabalho de conclus�o. Por gentileza contate o administrador do sistema.");
				responseJsonWithId.setId(0l);
			}
		} else {
			responseJsonWithId.setStatus("FAIL");

			String erros = "";

			if (result.getErrorCount() == 1) {
				erros = "O seguinte erro foi apresentado durante a valida��o dos dados: <br />";
			} else {
				erros = "Os seguintes erros foram apresentados durante a valida��o dos dados: <br />";
			}

			for (ObjectError erro : result.getAllErrors()) {
				erros += "<br />" + erro.getDefaultMessage();
			}

			responseJsonWithId.setResult(erros);
		}

		return responseJsonWithId;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/lista/trabalho_de_conclusao", method = RequestMethod.POST)
	public TableResponseJson listar(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		TableResponseJson res = new TableResponseJson();

		List<TrabalhoDeConclusao> listaTrabalhoDeConclusao = trabalhoDeConclusaoDAO.listar("");

		res.setData(listaTrabalhoDeConclusao);

		return res;
	}
	
	@ResponseBody
	@RequestMapping(value = "rest/lista/anexo/{id_tcc}", method = RequestMethod.POST)
	public TableResponseJson listarAnexo(HttpServletRequest req, @PathVariable("id_tcc") Long idTCC) {
		// Cria objeto de retorno do JSON
		TableResponseJson res = new TableResponseJson();
		
		String filtro = "";
		
		if(idTCC != 0){
			filtro = " WHERE t.id = " + idTCC;
		}

		List<Anexo> listaAnexos = trabalhoDeConclusaoDAO.listar(filtro).get(0).getListaAnexos();

		res.setData(listaAnexos);

		return res;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/exclui/trabalho_de_conclusao")
	public ResponseJson excluir(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		ResponseJson response = new ResponseJson();

		// Pega o c�digo do linhaDePesquisa que ser� excluido
		String id = req.getParameter("id");

		// Pega o objeto de linhaDePesquisa para pesquisar no banco
		TrabalhoDeConclusao trabalhoDeConclusao = trabalhoDeConclusaoDAO.pesquisarPorId(Integer.parseInt(id));

		for(int i = 0; i < trabalhoDeConclusao.getListaAlunos().size(); i++){
			trabalhoDeConclusao.getListaAlunos().get(i).setTrabalhoDeConclusao(null);
		}
		
		for(int i = 0; i < trabalhoDeConclusao.getListaAnexos().size(); i++){
			//Exclui o arquivo anexo ao excluir o Trabalho de Conclus�o
			File anexo = new File(trabalhoDeConclusao.getListaAnexos().get(i).getCaminho());
			try {
				FileUtils.deleteDirectory(anexo);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			anexoDAO.excluir(trabalhoDeConclusao.getListaAnexos().get(i));
		}
		
		trabalhoDeConclusao.setListaAnexos(new ArrayList<>());
		
		if(trabalhoDeConclusaoDAO.alterar(trabalhoDeConclusao)){
			// Realiza a exclus�o do linhaDePesquisa
			if (trabalhoDeConclusaoDAO.excluir(trabalhoDeConclusao)) {
				
				if(trabalhoDeConclusao.getMonografia() != null){
					//Exclui o arquivo de monografia ao excluir o Trabalho de Conclus�o
					File monografia = new File(trabalhoDeConclusao.getMonografia().getCaminho());
					try {
						FileUtils.deleteDirectory(monografia);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				response.setStatus("SUCCESS");
				response.setResult("Trabalho de conclus�o exclu�do com sucesso.");
			} else {
				response.setStatus("FAIL");
				response.setResult("Erro ao excluir o trabalho de conclus�o. Por gentileza contate o administrador do sistema.");
			}
		}else{
			response.setStatus("FAIL");
			response.setResult("Erro ao excluir o trabalho de conclus�o. Por gentileza contate o administrador do sistema.");
		}
		
		return response;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/inativa/trabalho_de_conclusao", method = RequestMethod.POST)
	public ResponseJson inativar(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		ResponseJson response = new ResponseJson();

		// Pega o c�digo do linhaDePesquisa que ser� inativado
		String id = req.getParameter("id");

		// Pega o objeto de linhaDePesquisa para alterar o status
		TrabalhoDeConclusao trabalhoDeConclusao = trabalhoDeConclusaoDAO.pesquisarPorId(Integer.parseInt(id));
		
		if(trabalhoDeConclusao.getMonografia() == null){
			response.setStatus("FAIL");
			response.setResult("N�o � poss�vel ativar o trabalho de conclus�o para visualiza��o. N�o existe arquivo de monografia associado para exibi��o. "
					+ "Por gentileza associe um arquivo de monografia para ativar a visualiza��o.");
			
			return response;
		}

		// Altera o status do linhaDePesquisa
		if (trabalhoDeConclusao.isAtivo()) {
			trabalhoDeConclusao.setAtivo(false);
		} else {
			trabalhoDeConclusao.setAtivo(true);
		}

		// Grava as altera��es realizadas com o linhaDePesquisa
		if (trabalhoDeConclusaoDAO.alterar(trabalhoDeConclusao)) {
			response.setStatus("SUCCESS");

			if (trabalhoDeConclusao.isAtivo()) {
				response.setResult("Trabalho de conclus�o ativada com sucesso.");
			} else {
				response.setResult("Trabalho de conclus�o inativada com sucesso.");
			}
		} else {
			response.setStatus("FAIL");
			response.setResult("Erro ao ativar/inativar o trabalho de conclus�o. Por gentileza contate o administrador do sistema.");
		}
		
		return response;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/json/trabalho_de_conclusao", method = RequestMethod.POST)
	public TrabalhoDeConclusao entidadeJSON(HttpServletRequest req) {
		// Pega o c�digo do TrabalhoDeConclusao que ser� inativado
		String id = req.getParameter("id");

		// Pega o objeto de linhaDePesquisa para alterar o status
		TrabalhoDeConclusao trabalhoDeConclusao = trabalhoDeConclusaoDAO.pesquisarPorId(Integer.parseInt(id));

		return trabalhoDeConclusao;
	}

	@RequestMapping(value = "adm/relatorio/pdf/trabalho_de_conclusao", method = RequestMethod.GET)
	public ModelAndView gerarRelatorio(ModelAndView modelAndView) {
		List<TrabalhoDeConclusao> listaTrabalhoDeConclusao = trabalhoDeConclusaoDAO.listar(" ORDER BY t.titulo");

		//Cria��o da DataSouce do iReport
		JRDataSource JRdataSource = new JRBeanCollectionDataSource(listaTrabalhoDeConclusao, false);
		
		//Map de par�metros a serem passados para o iReport
		Map<String, Object> parameterMap = new HashMap<String, Object>();
		parameterMap.put("datasource", JRdataSource);
		parameterMap.put("path", servletContext.getRealPath("/images/reports/"));
		
		Map<String, Object> exporterParameters = new HashMap<String, Object>();
		
		//Propriedades do Header da Response
		Properties header = new Properties();
		
		//Nome do arquivo caso o usu�rio de Ctrl+S (Salvar)
		header.put("Content-Disposition", "inline; filename=Trabalho de Conclusao.pdf");
		
		JasperReportsPdfView view = new JasperReportsPdfView();
		view.setUrl("/WEB-INF/reports/pdf/trabalho_de_conclusao.jrxml");
		view.setReportDataKey("datasource");
		view.setExporterParameters(exporterParameters);
		view.setApplicationContext(applicationContext);
		view.setHeaders(header);
		
		// Gera o relat�rio de acordo com a extensao enviada por par�metro de URL
		modelAndView = new ModelAndView(view, parameterMap);
		return modelAndView;
	}

}
