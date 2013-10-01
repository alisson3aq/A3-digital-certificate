package br.com.javac.nfeapplet.controller;

import java.io.IOException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;

import br.com.javac.nfeapplet.business.service.AssinarXML;
import br.com.javac.nfeapplet.business.service.CertificadoWindows;
import br.com.javac.nfeapplet.business.service.SefazService;
import br.com.javac.nfeapplet.entity.Certificado;
import br.com.javac.nfeapplet.util.Utils;
import br.com.javac.nfeapplet.view.NFe;

public class NFeController {
	private final static String MSG_SELECIONE_UM_CERTIFIADO = "Selecione um Certificado Digital. Para exibir os Certiticados Digitais clique no bot�o Listar Certificados.";

	private NFe view;
	private CertificadoWindows certificadoWindows;
	private SefazService sefazService;
	private List<Certificado> listaDeCertificados;

	public NFeController(NFe view) {
		this.view = view;
	}

	public void listaCerticados() {
		Thread processar = new Thread() {
			@SuppressWarnings("unchecked")
			@Override
			public void run() {
				try {
					clearMessages();

					listaDeCertificados = getCertificadoWindows().listaCertificadoDisponiveis();
					if ((getListaDeCertificados() != null) && (!getListaDeCertificados().isEmpty())) {
						@SuppressWarnings("rawtypes")
						DefaultListModel listModel = new DefaultListModel();
						for (Certificado certificado : getListaDeCertificados()) {
							listModel.addElement(certificado.getEmitidoPara());
						}
						getView().getListaCertificados().setModel(listModel);
					}
				} catch (Exception e) {
					getView().getTextInformacao().append(e.toString());
				}
			}
		};
		processar.start();
	}

	public void dadosDoCertificado() {
		Thread processar = new Thread() {
			@Override
			public void run() {
				try {
					clearMessages();
		            if ((getListaDeCertificados() != null) && (!getListaDeCertificados().isEmpty())) {
						int indexSelected = getView().getListaCertificados().getSelectedIndex();
		            	if (indexSelected != -1) {
			            	Certificado certificado = getListaDeCertificados().get(indexSelected);
							if (certificado != null) {
								getView().getTextInformacao().append("Emitido para: " + certificado.getEmitidoPara() + "\n\r");
								getView().getTextInformacao().append("Alias: " + certificado.getAlias() + "\n\r");
								getView().getTextInformacao().append("ValidoDe: " + certificado.getValidoDe() + "\n\r");
								getView().getTextInformacao().append("ValidoAte: " + certificado.getValidoAte());
							}
							else {
								throw new Exception("Certificado Digital n�o localizado.");
							}
		            	}
		            	else {
		            		throw new Exception(MSG_SELECIONE_UM_CERTIFIADO);
		            	}
					}
					else {
						throw new Exception(MSG_SELECIONE_UM_CERTIFIADO);
					}
				} catch (Exception e) {
					getView().getTextInformacao().append(e.getMessage());
				}
			}
		};
		processar.start();
	}

	public void assinarXml() {
		Thread processar = new Thread() {
			@Override
			public void run() {
				try {
					clearMessages();
					startProgressBar("Aguarde! Assinando XML...");
		            if ((getListaDeCertificados() != null) && (!getListaDeCertificados().isEmpty())) {
						int indexSelected = getView().getListaCertificados().getSelectedIndex();
		            	if (indexSelected != -1) {
		            		Certificado certificado = getListaDeCertificados().get(indexSelected);
		            		if (certificado != null) {
								JFileChooser fileChooser = new JFileChooser();
								int retorno = fileChooser.showOpenDialog(null);
								if (retorno ==  JFileChooser.APPROVE_OPTION) {
									String xmlPath = fileChooser.getSelectedFile().getAbsolutePath();
									if ((xmlPath != null) && (!"".equals(xmlPath))) {
										assinatura(certificado, xmlPath);
									}
								}
		            		}
							else {
								throw new Exception("Certificado Digital n�o localizado.");
							}
		            	}
		            	else {
		            		throw new Exception(MSG_SELECIONE_UM_CERTIFIADO);
		            	}
		            }
					else {
						throw new Exception(MSG_SELECIONE_UM_CERTIFIADO);
					}
				} catch (Exception e) {
					getView().getTextInformacao().append(e.getMessage());
				} finally {
					stopProgressBar();
				}
			}
		};
		processar.start();
	}

	public void consultaStatusDoServico() {
		Thread processar = new Thread() {
			@Override
			public void run() {
				try {
					String codigoDoEstado = "52";
		            URL url = new URL("https://homolog.sefaz.go.gov.br/nfe/services/v2/NfeStatusServico2");
		            clearMessages();
		            if ((getListaDeCertificados() != null) && (!getListaDeCertificados().isEmpty())) {
						int indexSelected = getView().getListaCertificados().getSelectedIndex();
		            	if (indexSelected != -1) {
				            String senha = new String(getView().getEdtSenhaDoCertificado().getPassword());
				            if ((senha == null) || ("".equals(senha))) {
				            	throw new Exception("Digite a senha do Certificado Digital.");
				            }

							Certificado certificado = getListaDeCertificados().get(indexSelected);
							if (certificado != null) {
								startProgressBar("Aguarde! Consultando Status do Servi�o...");
								getCertificadoWindows().loadWsCerticates(url, certificado.getAlias(), senha);
								String retorno = getSefazService().consultaStatusDoServico(codigoDoEstado, url);
								getView().getTextInformacao().append(retorno);
							}
							else {
								throw new Exception("Certificado Digital n�o localizado.");
							}
		            	}
		            	else {
		            		throw new Exception(MSG_SELECIONE_UM_CERTIFIADO);
		            	}
					}
					else {
						throw new Exception(MSG_SELECIONE_UM_CERTIFIADO);
					}
				} catch (Exception e) {
					getView().getTextInformacao().append(e.getMessage());
				} finally {
					stopProgressBar();
				}
			}
		};
		processar.start();
	}

	private void startProgressBar(String texto) {
		getView().getProgressBarStatus().setIndeterminate(true);
		getView().getProgressBarStatus().setString(texto);
		enableComponents(false);
	}

	private void stopProgressBar() {
		getView().getProgressBarStatus().setIndeterminate(false);
		getView().getProgressBarStatus().setString("");
		enableComponents(true);
	}

	private void enableComponents(boolean enable) {
		getView().getListaCertificados().setEnabled(enable);
		getView().getEdtSenhaDoCertificado().setEnabled(enable);
		getView().getBtnConsultarStatusServico().setEnabled(enable);
		getView().getBtnDadosDoCertificado().setEnabled(enable);
		getView().getBtnListarCertificados().setEnabled(enable);
		getView().getBtnAssinatura().setEnabled(enable);

		getView().getRbtLoteNfe().setEnabled(enable);
		getView().getRbtCancelamento().setEnabled(enable);
		getView().getRbtCce().setEnabled(enable);
		getView().getRbtDpec().setEnabled(enable);
		getView().getRbtInutilizacao().setEnabled(enable);
		getView().getTextInformacao().setEditable(enable);
	}

	private void assinatura(Certificado certificado, String xmlPath) throws IOException, KeyStoreException,
			NoSuchProviderException, NoSuchAlgorithmException, CertificateException, Exception {
		KeyStore keyStore = getCertificadoWindows().getKeyStore();
		String xmlAssinado = "";

		String xml = Utils.lerXML(xmlPath);
		xml = Utils.normalizeXML(xml);

		String alias = certificado.getAlias();
		String senha = new String(getView().getEdtSenhaDoCertificado().getPassword());
		AssinarXML assinarXML = new AssinarXML(keyStore, alias, senha);

		if (getView().getRbtLoteNfe().isSelected()) {
			xmlAssinado = assinarXML.assinaEnviNFe(xml);
		}
		else if (getView().getRbtCancelamento().isSelected()) {
			xmlAssinado = assinarXML.assinaCancNFe(xml);
		}
		else if (getView().getRbtCce().isSelected()) {
			xmlAssinado = assinarXML.assinaEnvEvento(xml);
		}
		else if (getView().getRbtDpec().isSelected()) {
			xmlAssinado = assinarXML.assinaEnvDPEC(xml);
		}
		else if (getView().getRbtInutilizacao().isSelected()) {
			xmlAssinado = assinarXML.assinaInutNFe(xml);
		}

		getView().getTextInformacao().append(xmlAssinado);
	}

	private void clearMessages() {
		getView().getTextInformacao().setText("");
	}

	public CertificadoWindows getCertificadoWindows() {
		if (certificadoWindows == null) {
			certificadoWindows = new CertificadoWindows();
		}
		return certificadoWindows;
	}

	public SefazService getSefazService() {
		if (sefazService == null) {
			sefazService = new SefazService();
		}
		return sefazService;
	}

	public List<Certificado> getListaDeCertificados() {
		return listaDeCertificados;
	}

	public NFe getView() {
		return view;
	}

}