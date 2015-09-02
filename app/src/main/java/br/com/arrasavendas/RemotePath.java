package br.com.arrasavendas;


import android.net.Uri;

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

    public static String getFullImagePath(String imageName){
        return Constants.host + "/images/produtos/"+ Uri.encode(imageName);
    }


    public String getUrl() {
		return url;
	}

    private static class Constants {

       private static final String host = Application.context().getResources().getString(R.string.remote_server);
    }
}
