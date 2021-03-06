package br.com.ctrlt.dao;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import br.com.ctrlt.model.AdministradorDeConteudo;

@Repository
@EnableTransactionManagement(proxyTargetClass = true)
public class AdministradorDeConteudoDAO implements DAO<AdministradorDeConteudo> {
	@PersistenceContext
	private EntityManager manager; // gerenciar as conexoes de banco de dados.

	@SuppressWarnings("unchecked")
	@Override
	public List<AdministradorDeConteudo> listar(String criterio) {
		Query query = manager.createQuery("SELECT a FROM AdministradorDeConteudo a " + criterio);

		List<AdministradorDeConteudo> listaAdministradoresDeConteudo = query.getResultList();

		return listaAdministradoresDeConteudo;
	}

	@Override
	public AdministradorDeConteudo pesquisarPorId(long id) {
		// O metodo find do hibernate ja pesquisa pela chave prim�ria.
		AdministradorDeConteudo administradorDeConteudo = manager.find(AdministradorDeConteudo.class, id);

		return administradorDeConteudo;
	}

	@Override
	@Transactional
	public boolean cadastrar(AdministradorDeConteudo administradorDeConteudo) {
		try {
			manager.persist(administradorDeConteudo); // o metodo persist faz o insert
		
			return true;
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}

	@Override
	@Transactional
	public boolean alterar(AdministradorDeConteudo administradorDeConteudo) {
		try {
			manager.merge(administradorDeConteudo); // o metodo merge faz a alteracao
		
			return true;
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}

	@Override
	@Transactional
	public boolean excluir(AdministradorDeConteudo administradorDeConteudo) {
		try {
			manager.remove(manager.merge(administradorDeConteudo)); 
	
			return true;
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}

	@Override
	@Transactional
	public boolean excluirPorId(int id) {
		AdministradorDeConteudo administradorDeConteudo = pesquisarPorId(id);
	
		try {
			manager.remove(administradorDeConteudo); 
	
			return true;
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}
	
	public AdministradorDeConteudo logar(String login, String senha){
		TypedQuery<AdministradorDeConteudo> query = manager.createQuery("SELECT a FROM AdministradorDeConteudo a "
				+ "WHERE a.login = :login "
				+ "AND a.senha = :senha", AdministradorDeConteudo.class)
				.setParameter("login", login)
				.setParameter("senha", senha);
		
		return query.getSingleResult();
	}
	
	public EntityManager getEntityManager() {
		return manager;
	}

}
