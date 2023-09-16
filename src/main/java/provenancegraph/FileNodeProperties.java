package provenancegraph;

import java.util.Objects;

import static utils.EntityGeneralizer.filePathGeneralizer;


public class FileNodeProperties extends NodeProperties {
    private String filePath;

    public FileNodeProperties(String filePath) {
        this.type = NodeType.File;
        this.filePath = filePath;
    }

    public String getFilePath(){
        return this.filePath;
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
                filePathGeneralizer(filePath)
        );
        return generalizedProperties;
    }
}