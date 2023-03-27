package tfc.plataforma.projeto_v1;

public class BuildingData {
    private String id;
    private String data;
    private float temp;
    private float luminosidade;
    private float humidade;

    public BuildingData(String id, String data, float temp, float luminosidade, float humidade){
        this.id = id;
        this.data = data;
        this.temp = temp;
        this.luminosidade = luminosidade;
        this.humidade = humidade;
    }

    public String getId(){return this.id;}
    public String getData(){return this.data;}
    public String getTemp(){return this.temp + "";}
    public String getLuminosidade(){return this.luminosidade+"";}
    public String getHumidade(){return this.humidade + "";}

}
