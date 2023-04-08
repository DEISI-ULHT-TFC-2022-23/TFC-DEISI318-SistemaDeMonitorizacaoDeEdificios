package tfc.plataforma.projeto_v1;

public class BuildingData {
    private String id;
    private String data;
    private double temp;
    private double luminosidade;
    private double humidade;

    public BuildingData(String id, String data, double temp, double luminosidade, double humidade){
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
