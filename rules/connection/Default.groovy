


if (context.from){
    if (context.to){
        //
        // Aqui Fazer a validação que fizer sentido
        //
        if (context.from.className =~ /State/){
            if (context.to.className =~ /City/){
                def m =["meuErro":"deu a louca no gerente"];
                context.dm.abortTransaction("Connection From State to City not Allowed",m);
            }
        }
        //
        //
        //
    }
}