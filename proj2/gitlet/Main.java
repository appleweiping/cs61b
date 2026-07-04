package gitlet;

/**
 * Driver class for gitlet, a subset of the Git version-control system.
 * Parses the command line, validates operands, and dispatches to
 * {@link Repository}.
 *
 * @author appleweiping
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS is a command followed by its
     *  operands. */
    public static void main(String[] args) {
        try {
            run(args);
        } catch (GitletException e) {
            System.out.println(e.getMessage());
            System.exit(0);
        }
    }

    private static void run(String[] args) {
        if (args.length == 0) {
            throw new GitletException("Please enter a command.");
        }
        String cmd = args[0];

        // init is the only command allowed before a repository exists.
        if (!cmd.equals("init") && !Repository.initialized()) {
            throw new GitletException("Not in an initialized Gitlet directory.");
        }

        switch (cmd) {
            case "init":
                validate(args, 1);
                Repository.init();
                break;
            case "add":
                validate(args, 2);
                Repository.add(args[1]);
                break;
            case "commit":
                validate(args, 2);
                Repository.commit(args[1]);
                break;
            case "rm":
                validate(args, 2);
                Repository.rm(args[1]);
                break;
            case "log":
                validate(args, 1);
                Repository.log();
                break;
            case "global-log":
                validate(args, 1);
                Repository.globalLog();
                break;
            case "find":
                validate(args, 2);
                Repository.find(args[1]);
                break;
            case "status":
                validate(args, 1);
                Repository.status();
                break;
            case "checkout":
                checkout(args);
                break;
            case "branch":
                validate(args, 2);
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                validate(args, 2);
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                validate(args, 2);
                Repository.reset(args[1]);
                break;
            case "merge":
                validate(args, 2);
                Repository.merge(args[1]);
                break;
            default:
                throw new GitletException("No command with that name exists.");
        }
    }

    /** Dispatch the three forms of checkout, validating operand shape. */
    private static void checkout(String[] args) {
        if (args.length == 3 && args[1].equals("--")) {
            // checkout -- [file name]
            Repository.checkoutFile(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            // checkout [commit id] -- [file name]
            Repository.checkoutFileFromCommit(args[1], args[3]);
        } else if (args.length == 2) {
            // checkout [branch name]
            Repository.checkoutBranch(args[1]);
        } else {
            throw new GitletException("Incorrect operands.");
        }
    }

    /** Require exactly N space-separated tokens in ARGS. */
    private static void validate(String[] args, int n) {
        if (args.length != n) {
            throw new GitletException("Incorrect operands.");
        }
    }
}
