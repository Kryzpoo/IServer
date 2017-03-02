class ErrorCollector {

    private String errorText = "";


    /*
     * Собирает все строки ошибок и помещает их в одну
     */
    void packErrorText( boolean value, String addErrorText ) {
        if ( !value ) {
            errorText += addErrorText + "\n";
        }
    }


    String getErrorText() {
        return errorText;
    }
}
