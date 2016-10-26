package br.com.ctrlt.controller;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
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
import org.springframework.web.servlet.view.jasperreports.JasperReportsPdfView;

import br.com.ctrlt.dao.AlunoDAO;
import br.com.ctrlt.dao.ProfessorDAO;
import br.com.ctrlt.dao.TrabalhoDeConclusaoDAO;
import br.com.ctrlt.json.ResponseJson;
import br.com.ctrlt.json.ResponseJsonWithId;
import br.com.ctrlt.json.TableResponseJson;
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
	
	@Override
	@RequestMapping(value = "adm/cadastro/trabalho_de_conclusao", method = RequestMethod.GET)
	public String carregarPagina(Model model) {
		model.addAttribute("alunos", alunoDAO.listar(" WHERE a.trabalhoDeConclusao = null AND a.ativo = true"));
		model.addAttribute("professores", professorDAO.listar(" WHERE p.ativo = true"));
		
		return "adm/cadastros/cadastro_trabalho_de_conclusao";
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
		entidade.setDataPublicao(Calendar.getInstance());
		
		if(entidade.getListaAlunos() != null){
			//Atualiza a lista de alunos
			for(int i = 0; i < entidade.getListaAlunos().size(); i++){
				if (entidade.getListaAlunos().get(i).getId() == 0){
					entidade.getListaAlunos().remove(i);
					i = 0;
				}else{
					entidade.getListaAlunos().set(i, alunoDAO.pesquisarPorId(entidade.getListaAlunos().get(i).getId()));
				}
			}
		}
		
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
		
		if (!result.hasErrors()) {
			entidade.setAtivo(true);

			if (trabalhoDeConclusaoDAO.cadastrar(entidade)) {
				responseJsonWithId.setStatus("SUCCESS");
				responseJsonWithId.setResult("Trabalho de conclus�o com sucesso.");
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
		
		if(! arquivo.isEmpty()){
			//Caminho absoluto do caminho at� a pasta da monografia
			String path = servletContext.getRealPath("/monografias/" + String.valueOf(idTcc) + "/");
			
			File file = new File(path);
			
			//Cria o diret�rio caso n�o exista
			if(! file.exists()){
				file.mkdirs();
			}
			
			try {			
				arquivo.transferTo(new File(path + arquivo.getOriginalFilename()));
				
				Monografia monografia = new Monografia();
				
				monografia.setCaminho(path + arquivo.getOriginalFilename());
				monografia.setTamanho(arquivo.getSize());
				monografia.setNome(arquivo.getOriginalFilename());
				monografia.setDataUpload(Calendar.getInstance());
				monografia.setNumeroDownload(0);
				monografia.setExtensao(arquivo.getContentType());
				monografia.setAtivo(true);
				
				TrabalhoDeConclusao tcc = trabalhoDeConclusaoDAO.pesquisarPorId(idTcc);
				tcc.setMonografia(monografia);
				
				if(trabalhoDeConclusaoDAO.alterar(tcc)){
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
	@RequestMapping(value = "rest/altera/trabalho_de_conclusao", method = RequestMethod.POST)
	public ResponseJson alterar(@Valid TrabalhoDeConclusao entidade, BindingResult result) {
		ResponseJson responseJson = new ResponseJson();

		if (!result.hasErrors()) {
			TrabalhoDeConclusao linhaDePesquisaBanco = trabalhoDeConclusaoDAO.pesquisarPorId(entidade.getId());

			entidade.setAtivo(linhaDePesquisaBanco.isAtivo());

			if (trabalhoDeConclusaoDAO.alterar(entidade)) {
				responseJson.setStatus("SUCCESS");
				responseJson.setResult("Trabalho de conclus�o alterado com sucesso.");
			} else {
				responseJson.setStatus("FAIL");
				responseJson.setResult("Erro ao alterar o trabalho de conclus�o. Por gentileza contate o administrador do sistema.");
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
	@RequestMapping(value = "rest/lista/trabalho_de_conclusao", method = RequestMethod.POST)
	public TableResponseJson listar(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		TableResponseJson res = new TableResponseJson();

		List<TrabalhoDeConclusao> listaTrabalhoDeConclusaos = trabalhoDeConclusaoDAO.listar("");

		res.setData(listaTrabalhoDeConclusaos);

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
		TrabalhoDeConclusao linhaDePesquisa = trabalhoDeConclusaoDAO.pesquisarPorId(Integer.parseInt(id));

		// Realiza a exclus�o do linhaDePesquisa
		if (trabalhoDeConclusaoDAO.excluir(linhaDePesquisa)) {
			response.setStatus("SUCCESS");
			response.setResult("Trabalho de conclus�o exclu�da com sucesso.");
		} else {
			response.setStatus("FAIL");
			response.setResult("Erro ao excluir a trabalho de conclus�o. Por gentileza contate o administrador do sistema.");
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
		TrabalhoDeConclusao linhaDePesquisa = trabalhoDeConclusaoDAO.pesquisarPorId(Integer.parseInt(id));

		// Altera o status do linhaDePesquisa
		if (linhaDePesquisa.isAtivo()) {
			linhaDePesquisa.setAtivo(false);
		} else {
			linhaDePesquisa.setAtivo(true);
		}

		// Grava as altera��es realizadas com o linhaDePesquisa
		if (trabalhoDeConclusaoDAO.alterar(linhaDePesquisa)) {
			response.setStatus("SUCCESS");

			if (linhaDePesquisa.isAtivo()) {
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
		TrabalhoDeConclusao linhaDePesquisa = trabalhoDeConclusaoDAO.pesquisarPorId(Integer.parseInt(id));

		return linhaDePesquisa;
	}

	@RequestMapping(value = "adm/relatorio/pdf/trabalho_de_conclusao", method = RequestMethod.GET)
	public ModelAndView gerarRelatorio(ModelAndView modelAndView) {
		List<TrabalhoDeConclusao> listaTrabalhoDeConclusao = trabalhoDeConclusaoDAO.listar(" ORDER BY l.nome");

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
