package br.com.arrasavendas;

public enum RemotePath {
	EstoqueList(Constants.host + "/api/estoque?format=json"),
	VendaPath(Constants.host + "/api/vendas?format=json"),
    LoginPath(Constants.host +  "/api/login");

    private String url;
	
	RemotePath(String url){
		this.url = url;
	}

    public static String getVendaEntityPath(Long vendaId){
        return Constants.host + "/api/vendas/"+vendaId;
    }


    public String getUrl() {
		return url;
	}

    private static class Constants {
        //private static final String host = "http://172.16.90.1:8080/arrasa-amiga";
        //private static final String host = "http://192.168.1.8:8080/arrasa-amiga";
        private static final String host = "http://www.arrasaamiga.com.br";
    }
}
