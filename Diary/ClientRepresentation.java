package Diary;
import java.io.Serializable;

public class ClientRepresentation implements Serializable{
    private String adresse;
    private Integer port;

    public ClientRepresentation(String adresse, Integer port) {
        this.adresse = adresse;
        this.port = port;
    }

    public boolean equals(ClientRepresentation client) {
        return (this.adresse.equals(client.adresse) && this.port.equals(client.port));
    }

    public String getAdresse() {
        return this.adresse;
    }

    public Integer getPort() {
        return this.port;
    }

}