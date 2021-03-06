package br.com.ctrlt.controller;

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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.jasperreports.JasperReportsPdfView;

import br.com.ctrlt.dao.CursoDAO;
import br.com.ctrlt.dao.PeriodoDAO;
import br.com.ctrlt.json.ResponseJson;
import br.com.ctrlt.json.TableResponseJson;
import br.com.ctrlt.model.Curso;
import br.com.ctrlt.model.Periodo;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@Controller
public class PeriodoController implements Control<Periodo> {

	@Autowired
	private ApplicationContext applicationContext;
	
	@Autowired
    private ServletContext servletContext; 
	
	@Autowired
	private PeriodoDAO periodoDAO;
	
	@Autowired
	private CursoDAO cursoDao;
	
	@Override
	@RequestMapping(value = "adm/cadastro/periodo", method = RequestMethod.GET)
	public String carregarPagina(Model model) {
		return "adm/cadastros/cadastro_periodo";
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/cadastra/periodo", method = RequestMethod.POST)
	public ResponseJson cadastrar(@Valid Periodo entidade, BindingResult result) {
		ResponseJson responseJson = new ResponseJson();

		if (!result.hasErrors()) {		
			entidade.setAtivo(true);
			
			if (periodoDAO.cadastrar(entidade)) {
				responseJson.setStatus("SUCCESS");
				responseJson.setResult("Periodo cadastrado com sucesso.");
			} else {
				responseJson.setStatus("FAIL");
				responseJson.setResult("Erro ao cadastrar o periodo. Por gentileza contate o administrador do sistema.");
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
	@RequestMapping(value = "rest/altera/periodo", method = RequestMethod.POST)
	public ResponseJson alterar(@Valid Periodo entidade, BindingResult result) {
		ResponseJson responseJson = new ResponseJson();

		if (!result.hasErrors()) {
			Periodo periodo = periodoDAO.pesquisarPorId(entidade.getId());
			
			entidade.setAtivo(periodo.isAtivo());
			
			if (periodoDAO.alterar(entidade)) {
				responseJson.setStatus("SUCCESS");
				responseJson.setResult("Periodo alterado com sucesso.");
			} else {
				responseJson.setStatus("FAIL");
				responseJson.setResult("Erro ao alterar o periodo. Por gentileza contate o administrador do sistema.");
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
	@RequestMapping(value = "rest/lista/periodo", method = RequestMethod.POST)
	public TableResponseJson listar(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		TableResponseJson res = new TableResponseJson();

		List<Periodo> listaPeriodos = periodoDAO.listar("");

		res.setData(listaPeriodos);

		return res;
	}

	@Override
	@SuppressWarnings("unchecked")
	@ResponseBody
	@RequestMapping(value = "rest/exclui/periodo", method = RequestMethod.POST)
	public ResponseJson excluir(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		ResponseJson response = new ResponseJson();

		// Pega o c�digo do periodo que ser� excluido
		String id = req.getParameter("id");

		// Pega o objeto de periodo para pesquisar no banco
		Periodo periodo = periodoDAO.pesquisarPorId(Integer.parseInt(id));

		// Query para verificar se existe algum Professor associado com a linhaDePesquisa selecionada
		List<Curso> listaCursos = cursoDao
				.getEntityManager().createQuery("SELECT DISTINCT c FROM Curso c "
						+ " JOIN FETCH c.listaPeriodo p " 
						+ " WHERE p = :periodo")
				.setParameter("periodo", periodo).getResultList();

		if (listaCursos.size() > 0) {
			response.setStatus("FAIL");
			response.setResult("N�o � poss�vel excluir o per�odo selecionado porque existe(m) cursos(s) "
					+ "associado(s) a este per�odo. <br /><br />"
					+ "Por gentileza realize as desassocia��es antes de excluir o per�odo.");
		} else {
			// Realiza a exclus�o do periodo
			if (periodoDAO.excluir(periodo)) {
				response.setStatus("SUCCESS");
				response.setResult("Periodo exclu�do com sucesso.");
			} else {
				response.setStatus("FAIL");
				response.setResult("Erro ao excluir o periodo. Por gentileza contate o administrador do sistema.");
			}
		}

		return response;
	}

	@Override
	@SuppressWarnings("unchecked")
	@ResponseBody
	@RequestMapping(value = "rest/inativa/periodo", method = RequestMethod.POST)
	public ResponseJson inativar(HttpServletRequest req) {
		// Cria objeto de retorno do JSON
		ResponseJson response = new ResponseJson();

		// Pega o c�digo do periodo que ser� inativado
		String id = req.getParameter("id");

		// Pega o objeto de periodo para alterar o status
		Periodo periodo = periodoDAO.pesquisarPorId(Integer.parseInt(id));

		// Query para verificar se existe algum Professor associado com a linhaDePesquisa selecionada
		List<Curso> listaCursos = cursoDao
				.getEntityManager().createQuery("SELECT DISTINCT c FROM Curso c "
						+ " JOIN FETCH c.listaPeriodo p " 
						+ " WHERE p = :periodo AND "
						+ " c.ativo = :ativo")
				.setParameter("periodo", periodo)
				.setParameter("ativo", true).getResultList();

		if (listaCursos.size() > 0) {
			response.setStatus("FAIL");
			response.setResult("N�o � poss�vel inativar este per�odo selecionada porque existe(m) "
					+ "curso(s) ativo(s) associado(s) a este per�odo. <br /><br />"
					+ "Por gentileza realize as desassocia��es antes de inativar o per�odo.");
		}else{
			// Altera o status do periodo
			if (periodo.isAtivo()) {
				periodo.setAtivo(false);
			} else {
				periodo.setAtivo(true);
			}
	
			// Grava as altera��es realizadas com o periodo
			if (periodoDAO.alterar(periodo)) {
				response.setStatus("SUCCESS");
	
				if (periodo.isAtivo()) {
					response.setResult("Periodo ativado com sucesso.");
				} else {
					response.setResult("Periodo inativado com sucesso.");
				}
			} else {
				response.setStatus("FAIL");
				response.setResult("Erro ao ativar/inativar o periodo. Por gentileza contate o administrador do sistema.");
			}
		}

		return response;
	}

	@Override
	@ResponseBody
	@RequestMapping(value = "rest/json/periodo", method = RequestMethod.POST)
	public Periodo entidadeJSON(HttpServletRequest req) {
		// Pega o c�digo do Periodo que ser� inativado
		String id = req.getParameter("id");

		// Pega o objeto de periodo para alterar o status
		Periodo periodo = periodoDAO.pesquisarPorId(Integer.parseInt(id));

		return periodo;
	}

	@RequestMapping(value = "adm/relatorio/pdf/periodo", method = RequestMethod.GET)
	public ModelAndView gerarRelatorio(ModelAndView modelAndView) {
		List<Periodo> listaPeriodo = periodoDAO.listar(" ORDER BY p.nome");

		//Cria��o da DataSouce do iReport
		JRDataSource JRdataSource = new JRBeanCollectionDataSource(listaPeriodo, false);
		
		//Map de par�metros a serem passados para o iReport
		Map<String, Object> parameterMap = new HashMap<String, Object>();
		parameterMap.put("datasource", JRdataSource);
		parameterMap.put("path", servletContext.getRealPath("/images/reports/"));
		
		Map<String, Object> exporterParameters = new HashMap<String, Object>();
		
		//Propriedades do Header da Response
		Properties header = new Properties();
		
		//Nome do arquivo caso o usu�rio de Ctrl+S (Salvar)
		header.put("Content-Disposition", "inline; filename=Per�odos.pdf");
		
		JasperReportsPdfView view = new JasperReportsPdfView();
		view.setUrl("/WEB-INF/reports/pdf/periodo.jrxml");
		view.setReportDataKey("datasource");
		view.setExporterParameters(exporterParameters);
		view.setApplicationContext(applicationContext);
		view.setHeaders(header);
		
		// Gera o relat�rio de acordo com a extensao enviada por par�metro de URL
		modelAndView = new ModelAndView(view, parameterMap);
		return modelAndView;
	}

}
