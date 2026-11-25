package hei.school;

public class ConsoleSmsSender implements SmsSender {

    @Override
    public void send(String phoneNumber, String message) {
        System.out.println("SMS simulé à " + phoneNumber + " : " + message);
    }
}
