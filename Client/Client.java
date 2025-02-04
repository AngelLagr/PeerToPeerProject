package Client;

import Diary.DiaryRemote;
import Downloader.Downloader;
import Downloader.ExceptionFichierVide;
import Downloader.ExceptionPlusDeClient;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.Scanner;

public class Client implements Runnable,Serializable {
    private int id;
    private String ip;
    private DiaryRemote diary; // Référence RMI vers le Diary
    private Deamon deamon;
    private Downloader downloader;
    private String diary_ip;
    public Integer delay;

    public Client(int id, String diary_ip, Integer delay) {
        this.id = id;
        this.diary_ip = diary_ip;
        this.ip = getIp();
        this.delay = delay;
        try {
            this.deamon = new Deamon(this);
            this.downloader = new Downloader(this);

            // Connexion au Diary via RMI
            Registry reg = LocateRegistry.getRegistry(this.diary_ip, 1099);
            this.diary = (DiaryRemote) reg.lookup("DiaryService");

        } catch (Exception e) {
            System.out.println("Erreur lors de la connexion au Diary : " + e.getMessage());
        }
    }

    public String getIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();

        } catch (Exception e) {
            System.out.println("Erreur dans la récupération de l'ip");
            return "";
        }

    }

    public String getDiaryIp() {
        return this.diary_ip;
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java Client <id> <delay> <ip du Diary>");
            System.exit(1);
        }

        int id = Integer.parseInt(args[0]);
        int delay = Integer.parseInt(args[1]);
        String diary_ip = args[2];

        // Lancer le client avec l'id et les fichiers
        Client client = new Client(id, diary_ip, delay);
        new Thread(client).start();
    }


    public Deamon getDeamon(){
        return this.deamon;
    } 

    public int getId() {
        return this.id;
    }

    public void run() {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("Client démarré. Entrez une commande (getAnnuaire, getFichier <nom>, addFichier <chemin>, addDossier <chemin>, exit) :");

            while (true) {
                System.out.print("> ");
                String command = scanner.nextLine();

                if (command.equals("exit")) {
                    System.out.println("Arrêt du client.");
                    break;
                } else if (command.equals("getAnnuaire")) {
                    showAnnuaire();
                } else if (command.startsWith("getFichier ")) { //getfichier test.txt
                    long startTime = System.currentTimeMillis();

                    getFichier(command);
                    long endTime = System.currentTimeMillis();
                    System.out.println("\nDurée du téléchargement : " + (endTime-startTime) +"ms");
                } else if (command.startsWith("addFichier")) {
                    String filePath = command.substring(11).trim();
                    addFichier(filePath);
                } else if (command.startsWith("addDossier")) {
                    String filePath = command.substring(11).trim();
                    addDossier(filePath);
                } else {
                    System.out.println("Commande non reconnue.");
                }
            }
        }
    }   

    /**
     * Affiche l'annuaire des fichiers disponibles.
     */
    public void showAnnuaire() {
        try {
            System.out.println("Fichiers disponibles dans le Diary :");
            for (String file : diary.getAllFiles()) {
                System.out.println("- " + file + " de taille " + diary.getFileSizeDiary(file) + " détenu par : "+ diary.getClient(file));
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la récupération de l'annuaire : " + e.getClass());
        }
    }

    public void getFichier(String command) {
        long startTime = System.currentTimeMillis();
        long endTime = System.currentTimeMillis();
        String file_name = command.substring(11).trim();

        byte[] completeFile;
        try {
            
            completeFile = downloader.download(file_name);
            
            // Créer un fichier local et y écrire le contenu téléchargé
            File newFile = new File("./downloads/" + file_name);
            try (FileOutputStream fileOutputStream = new FileOutputStream(newFile)) {
                fileOutputStream.write(completeFile);
                System.out.println("Fichier téléchargé et enregistré sous : " + newFile.getAbsolutePath());
            } catch (Exception e) {
                System.out.println("Erreur lors de l'enregistrement du fichier : " + e.getClass());
            }
            endTime = System.currentTimeMillis();
        } catch (ExceptionPlusDeClient e) {
            System.out.println("Le fichier n'est plus téléchargable sur le réseau, car plus aucun client ne le possède " + e.getClass());
        } catch (ExceptionFichierVide e) {
            try {
                File file = new File("./downloads/" + file_name);
                if (!file.exists()) {
                    // Si le fichier n'existe pas, on le créer
                    if (file.createNewFile()) {
                        System.out.println("Fichier créé : " + file.getAbsolutePath());
                    }
                } else {
                    // Si le fichier existe, le rendre vide
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        System.out.println("Fichier existant vidé : " + file.getAbsolutePath());
                    }
                }
                endTime = System.currentTimeMillis();
            } catch (Exception b) {
                endTime = System.currentTimeMillis();
                System.out.println("Erreur lors de la création du fichier : " + b.getClass());
            }
        } catch (Exception e){
            endTime = System.currentTimeMillis();
            System.out.println("Erreur de connexion lors du téléchargement : " + e.getClass());
        }
        System.out.println("\nDurée du téléchargement complet : " + (endTime-startTime) +"ms");

    }

    // Cette methode permet au client de notifier qu'il possède un fichier
    public void addFichier(String filePath) {
        try {
            File file = new File(filePath);
            if (file.exists()) {
                try {
                    this.diary.addFiles(file.getName(), this.ip, this.deamon.getPort(), file.length());
                    this.deamon.addFile(file);
                } catch(Exception e) {
                    System.out.println("Erreur lors de la récupération du nombre de ligne d'une fichier avec le demon : " + e.getClass());
                }
            } else {
                System.out.println("Il n'y a pas de fichier au chemin : " + filePath);
            }  
        } catch (Exception e) {
            System.out.println("Remote Exception" + e.getClass());
        }
        
    }


    public void addDossier(String filePath) {
        File dossier = new File(filePath);
        if (!dossier.exists()) {
            System.out.println("Il n'y a pas de dossier ou de fichier au chemin : " + filePath);
        }
        if (dossier.isDirectory()) {
            File[] fichiers = dossier.listFiles();
            if (fichiers != null) {
                for (File fichier : fichiers) {
                    if (!fichier.isDirectory()) {
                        addFichier(fichier.getAbsolutePath());
                    } else {
                        addDossier(fichier.getAbsolutePath());
                    }
                }
            }
        } else {
            System.out.println("Le chemin spécifié n'est pas un dossier valide.");
        }
    }

    public void print(String texte) {
        System.out.println(texte);
    }

    public DiaryRemote getDiary() {
        return this.diary;
    }
}

  