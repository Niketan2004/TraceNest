import { Link } from "@tanstack/react-router";
import logoAsset from "../../assets/TraceNest logo.png";

export function Footer() {
  return (
    <footer className="border-t border-border/60 bg-background">
      <div className="mx-auto grid max-w-7xl gap-8 px-4 py-12 sm:px-6 md:grid-cols-4">
        <div className="md:col-span-2">
          <div className="flex items-center gap-2">
            <img src={logoAsset} alt="TraceNest" className="h-8 w-auto" />
            <span className="text-lg font-bold">
              <span className="text-brand-blue">Trace</span>
              <span className="text-brand-green">Nest</span>
            </span>
          </div>
          <p className="mt-3 max-w-sm text-sm text-muted-foreground">
            Connecting communities to reunite what matters — missing persons, lost pets, and
            vehicles.
          </p>
        </div>
        <div>
          <h4 className="text-sm font-semibold">Platform</h4>
          <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
            <li>
              <Link to="/reports" className="hover:text-foreground">
                Browse reports
              </Link>
            </li>
            <li>
              <Link to="/reports/map" className="hover:text-foreground">
                Map view
              </Link>
            </li>
            <li>
              <Link to="/register" className="hover:text-foreground">
                Create account
              </Link>
            </li>
          </ul>
        </div>
        <div>
          <h4 className="text-sm font-semibold">Company</h4>
          <ul className="mt-3 space-y-2 text-sm text-muted-foreground">
            <li>
              <a href="#" className="hover:text-foreground">
                About
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-foreground">
                Privacy
              </a>
            </li>
            <li>
              <a href="#" className="hover:text-foreground">
                Terms
              </a>
            </li>
          </ul>
        </div>
      </div>
      <div className="border-t border-border/60 py-5 text-center text-xs text-muted-foreground">
        © {new Date().getFullYear()} TraceNest · Built with care for our communities.
      </div>
    </footer>
  );
}
