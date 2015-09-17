package br.com.arrasavendas;


import android.net.Uri;

public enum RemotePath {
	EstoquePath(Constants.host + "/api/estoque"),
	VendaPath(Constants.host + "/api/vendas"),
    LoginPath(Constants.host +  "/api/login");

    private String url;
	
	RemotePath(String url){
		this.url = url;
	}

    public static String getEntityPath(RemotePath remotePath, Long id){
        return String.format("%s/%d", remotePath.getUrl(), id);
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
