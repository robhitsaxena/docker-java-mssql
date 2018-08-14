import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.core.command.PullImageResultCallback;
import org.apache.log4j.BasicConfigurator;

import java.util.concurrent.TimeUnit;

public class teatjavadockerclient {

    public static void main(String[] args) throws InterruptedException {
        BasicConfigurator.configure();
        DockerClient dockerClient
                = DockerClientBuilder.getInstance("tcp://localhost:1234").build();

        Volume volume1 = new Volume("/var/opt/mssql/backup"); //target

        //Network Creation
        /*
        CreateNetworkResponse networkResponse
                = dockerClient.createNetworkCmd()
                .withName("java-docker-mssql")
                .withDriver("bridge").exec();
                */


        //Pulling an image
        dockerClient.pullImageCmd("microsoft/mssql-server-linux")
                .withTag("latest")
                .exec(new PullImageResultCallback())
                .awaitCompletion(30, TimeUnit.SECONDS);


        //Container Creation
        CreateContainerResponse container
                = dockerClient.createContainerCmd("microsoft/mssql-server-linux:2017-latest")
                .withPortBindings(PortBinding.parse("1433:1433"))
                .withEnv("ACCEPT_EULA=Y", "SA_PASSWORD=P@ssw0rd")
                .withVolumes(volume1)
                .withBinds(new Bind("/Users/robhit_saxena/Downloads/test-bind", volume1)) //is source
                .withName("mssql-from-java")
                .withNetworkMode("java-docker-mssql")
                .exec();

        //Starting a container
        dockerClient.startContainerCmd(container.getId()).exec();
        String containerId = container.getId();


        //Executing commands in a running container

        ExecCreateCmdResponse execCreateCmdResponse = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("bash", "-c", "mkdir -p /var/opt/mssql/backup")
                .exec();


        dockerClient.execStartCmd(execCreateCmdResponse.getId()).exec(
                new ExecStartResultCallback(System.out, System.err)).awaitCompletion();

        ExecCreateCmdResponse execCreateCmdResponse1 = dockerClient.execCreateCmd(container.getId())
                .withAttachStdout(true)
                .withAttachStderr(true)
                .withCmd("bash", "-c", "/opt/mssql-tools/bin/sqlcmd -S localhost -U SA -P 'P@ssw0rd'")
                .exec();


        dockerClient.execStartCmd(execCreateCmdResponse1.getId()).exec(
                new ExecStartResultCallback(System.out, System.err)).awaitCompletion();



    }
}
