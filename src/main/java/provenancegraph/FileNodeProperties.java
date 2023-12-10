package provenancegraph;

import java.util.Objects;

import static utils.EntityGeneralizer.filePathGeneralizer;


public class FileNodeProperties extends NodeProperties {
    private String filePath;
    private String filePathHash;

    public FileNodeProperties(String filePath, String h) {
        this.type = NodeType.File;
        this.filePath = filePath;
        this.filePathHash = h;
    }

    public String getFilePath(){
        return this.filePath;
    }

    public String getFilePathHash(){
        return this.filePathHash;
    }

    @Override
    public String toString() {
        return String.format("[%s: PATH-%s]", this.type.toString(), this.filePath);
    }

    public boolean haveSameProperties(NodeProperties np) {
        FileNodeProperties that = (FileNodeProperties) np;
        return filePath.equals(that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), filePath);
    }

    public void generalize() {
        filePath = filePathGeneralizer(filePath);
    }

    @Override
    public FileNodeProperties copyGeneralize() {
        FileNodeProperties generalizedProperties = new FileNodeProperties(
                filePathGeneralizer(filePath),
                this.filePathHash
        );
        return generalizedProperties;
    }
}